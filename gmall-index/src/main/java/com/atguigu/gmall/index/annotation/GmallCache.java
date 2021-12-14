package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

/**
 * @author saberlin
 * @create 2021/12/14 0:17
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存数据的key的前缀
     * 默认gmall:
     * @return
     */
    String prefix() default "gmall:";

    /**
     * 缓存的过期时间，单位：min
     * 默认5min
     * @return
     */
    int timeout() default 5;

    /**
     * 为了防止缓存雪崩，给缓存时间添加随机值
     * 这里可以指定随机值范围，默认5min
     * @return
     */
    int random() default 5;

    /**
     * 为了防止缓存击穿，添加分布式锁
     * 这里可以给分布式锁指定前缀。默认：gmall:lock:
     * @return
     */
    String lock() default "gmall:lock:";
}
