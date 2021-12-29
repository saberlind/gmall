package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author saberlin
 * @create 2021/12/14 0:18
 */
@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter bloomFilter;

    //@Before("execution(* com.atguigu.gmall.index.service.*.*(..))")

    @Pointcut("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    void pointcut(){
    }

    /**
     * 1.必须返回Object对象
     * 2.必须有一个ProceedingJoinPoint
     * 3.必须抛出一个Throwable异常
     * 4.必须手动执行目标方法
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取切点方法的签名
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        // 获取方法对象
        Method method = signature.getMethod();
        // 获取方法上指定的注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取注解中的前缀
        String prefix = gmallCache.prefix();
        // 获取方法的参数
        //Object[] args = joinPoint.getArgs();
        //String param = Arrays.asList(args).toString();
        String param = StringUtils.join(joinPoint.getArgs(), ",");
        String key = prefix + param;
        // 获取方法的返回值类型
        Class<?> returnType = method.getReturnType();

        // 为了防止缓存穿透，使用布隆过滤器判断数据是否存在，不存在直接拦截
        if (!bloomFilter.contains(key)){
            return null;
        }

        // 1.先查缓存,如果缓存命中就直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,returnType);
        }
        // 2.为了防止缓存击穿，添加分布式锁
        RLock fairLock = this.redissonClient.getFairLock(gmallCache.lock() + param);
        fairLock.lock();
        try {
            // 3.再查缓存：因为在获取锁的过程中，可能有其他请求把数据放入缓存
            String json2 = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(json2)){
                return JSON.parseObject(json2,returnType);
            }

            // 4.执行目标方法
            Object result = joinPoint.proceed(joinPoint.getArgs());

            if (result!=null){
                // 5.为了防止缓存雪崩，给缓存时间添加随机值
                int timeout =  gmallCache.timeout() + new Random().nextInt(gmallCache.random());
                this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeout, TimeUnit.MINUTES);
            }
            return result;
        } finally {
            fairLock.unlock();
        }
    }
}

