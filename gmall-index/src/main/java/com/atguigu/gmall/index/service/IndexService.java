package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.redisson.RedissonRedLock;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author saberlin
 * @create 2021/12/12 18:35
 */
@Service
public class IndexService {

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 前缀设置： 模块名:缓存模型
    private static final String KEY_PREFIX = "index:cates:";
    private static final String LOCK_PREFIX = "index:cate:lock:";

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 129600,random = 14400,lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLv23CategoriesByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLv23CategoriesByPid(pid);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryLv23CategoriesByPid2(Long pid) {
        // 1. 先查询缓存，如果缓存命中，则直接返回
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isBlank(json)){
            return JSON.parseArray(json,CategoryEntity.class);
        }

        RLock lock = this.redissonClient.getLock(LOCK_PREFIX + pid);
        lock.lock();
        try {
            // 在获取锁的过程中，可能有其他请求已经把数据放入缓存
            String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(json2)){
                return JSON.parseArray(json2,CategoryEntity.class);
            }

            // 2. 如果缓存没有命中，则远程调用
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLv23CategoriesByPid(pid);
            List<CategoryEntity> categoryEntities = listResponseVo.getData();
            // 为了防止缓存穿透，数据即使为空也缓存，但是缓存时间一般不超过5分钟
            if (CollectionUtils.isEmpty(categoryEntities)){
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(categoryEntities),5, TimeUnit.MINUTES);
            }else {
                // 为了防止缓存雪崩，给缓存时间添加随机值
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(categoryEntities),90 + new Random().nextInt(10),TimeUnit.DAYS);
            }
            return categoryEntities;
        } finally {
            lock.unlock();
        }
    }

    public void testLock() throws InterruptedException {
        // 加锁：setnx
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock",uuid, 30,TimeUnit.SECONDS);
        if (!lock){
            // 重试
            Thread.sleep(100);
            testLock();
        }else {
            // 添加过期时间防止死锁
            // this.redisTemplate.expire("lock",30,TimeUnit.SECONDS);

            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)){
                this.redisTemplate.opsForValue().set("num","1");
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));

            // 解锁：del
            /*if (StringUtils.equals(uuid,this.redisTemplate.opsForValue().get("lock"))){
                this.redisTemplate.delete("lock");
            }*/

            // 解锁：del

            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                    "   return redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +
                    "end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
        }
    }

    public void testReInLock() throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        // 加锁
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30);

        if (lock){
            //读取redis中的num值
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)){
                this.redisTemplate.opsForValue().set("num","1");
            }
            Integer num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));

            //子业务:测试可重入性
            this.subLock(uuid);

            // 延时为了查看自动续期的效果
            TimeUnit.SECONDS.sleep(1000);

            this.distributedLock.unlock("lock",uuid);
        }
    }

    public void subLock(String uuid){
        this.distributedLock.tryLock("lock",uuid,30);
        //TODO: 业务操作
        this.distributedLock.unlock("lock",uuid);
    }


    public void testRedission(){
        // 只要锁的名字相同就是同一把锁
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();

        try {
            // 查询redis中的num值
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)){
                return;
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
        } finally {
            lock.unlock();
        }
    }

    public String testReentrantLock() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        lock.lock();

        boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
        if (res){
            try {
                return "ok";
            }finally {
                lock.unlock();
            }
        }else {
            return "false";
        }
    }

    public void testReadLock() throws InterruptedException {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("anyRWLock");
        // 最常见的使用方法
        rwLock.readLock().lock(5,TimeUnit.SECONDS);
        // TODO 业务
        // rwLock.writeLock().unlock
    }
    public void testWriteLock() throws InterruptedException {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("anyRWLock");
        // 最常见的使用方法
        rwLock.writeLock().lock(5,TimeUnit.SECONDS);
        // TODO 业务
        // rwLock.writeLock().unlock
    }

    public void testRedLock(){
        RLock redLock1 = this.redissonClient.getLock("lock1");
        RLock redLock2 = this.redissonClient.getLock("lock2");
        RLock redLock3 = this.redissonClient.getLock("lock3");
        RedissonRedLock redissonRedLock = new RedissonRedLock(redLock1, redLock2, redLock3);
        // 同时加锁，lock1,lock2,lock3
        // 红锁在大部分节点上加锁成功就算成功
        redissonRedLock.lock();
        // TODO 业务
        // lock.unlock();
    }

    public void testSemaphore1() throws InterruptedException {
        RSemaphore semaphore = this.redissonClient.getSemaphore("semaphore");
        try {
            semaphore.acquire();
        } finally {
            semaphore.release();
        }
    }

    public void testSemaphore2() throws InterruptedException {
        RSemaphore semaphore = this.redissonClient.getSemaphore("semaphore");
        semaphore.tryAcquire(10,TimeUnit.SECONDS);
    }

    public void testCountDown(){
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("countDownLatch");
        countDownLatch.countDown();
        System.out.println("出来一个人");
    }

    public void testLatch(){
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("countDownLatch");
        try {
            countDownLatch.trySetCount(6);
            countDownLatch.await();
            System.out.println("关门了。。。。。");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
