package com.atguigu.gmall.index.config;

import org.checkerframework.checker.units.qual.C;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author saberlin
 * @create 2021/12/13 23:20
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        // 可以用“redis://“来启用SSL链接
        config.useSingleServer().setAddress("redis://42.192.128.199:6379").setPassword("lindonga");
        return Redisson.create(config);
    }
}
