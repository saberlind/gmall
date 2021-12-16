package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @author saberlin
 * @create 2021/12/16 11:17
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService executorService(){
        return new ThreadPoolExecutor(100,200,600, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000), Executors.defaultThreadFactory(),
                (Runnable r, ThreadPoolExecutor executor)->{
                    System.out.println("异步任务失败");
        });
    }
}
