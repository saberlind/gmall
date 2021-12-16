package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author saberlin
 * @create 2021/12/13 19:16
 */
@Component
@Scope("prototype")
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Timer timer;

    /**
     * 加锁
     * @param lockName
     * @param uuid
     * @param expire
     * @return
     */
    public Boolean tryLock(String lockName,String uuid,Integer expire){
        String script = "if redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1 "+
                        "then " +
                        "   redis.call('hincrby',KEYS[1],ARGV[1],1) " +
                        "   redis.call('expire',KEYS[1],ARGV[2]) " +
                        "   return 1 " +
                        "else " +
                        "   return 0 " +
                        "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
        if (!flag){
            try{
                Thread.sleep(80);
                tryLock(lockName,uuid,expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            this.renewExpire(lockName,uuid,expire);
        }
        // 获取到锁，返回true
        return true;
    }

    public void unlock(String lockName,String uuid){
        String script = "if redis.call('hexists',KEYS[1],ARGV[1])==0 "+
                        "then "+
                        "   return nil " +
                        "elseif redis.call('hincrby',KEYS[1],ARGV[1],-1)==0 "+
                        "then "+
                        "   return redis.call('del',KEYS[1]) "+
                        "else "+
                        "   return 0 " +
                        "end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if (flag == null){
            throw new RuntimeException("你释放的锁不存在或者不属于你！");
        }else if (flag == 1){
            this.timer.cancel();
        }
    }

    /**
     * 重置过期时间
     * @param lockName
     * @param uuid
     * @param expire
     */
    private void renewExpire(String lockName, String uuid, Integer expire) {
        String script = "if redis.call('hexists',KEYS[1],ARGV[1]) ==1 " +
                "then " +
                "   return redis.call('expire',KEYS[1],ARGV[2]) " +
                "else " +
                "   return 0 " +
                "end ";
        // 使用定时器，延时(delay)时间，并在过期时间进行到三分之一的时候进行自动续期
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
            }
        },expire * 1000 / 3 ,expire * 1000 / 3);
    }
}
