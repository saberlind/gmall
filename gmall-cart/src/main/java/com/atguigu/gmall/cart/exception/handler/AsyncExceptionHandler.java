package com.atguigu.gmall.cart.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author saberlin
 * @create 2021/12/20 10:09
 */
@Component
@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("异步任务出错：{}，方法：{},参数：{}",throwable.getMessage(),method.getName(), Arrays.asList(objects));

        // 把异常用户信息存入redis,通过定时任务同步数据
        String userId = objects[0].toString();
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(KEY);
        setOps.add(userId);
    }
}
