package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.exception.handler.AsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.*;

/**
 * @author saberlin
 * @create 2021/12/20 10:12
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Autowired
    private AsyncExceptionHandler asyncExceptionHandler;

    /**
     * 配置线程池方法
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(40, 100, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000), Executors.defaultThreadFactory(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                throw new RuntimeException("异步任务执行失败");
            }
        });
        return threadPool;
    }

    /**
     * 配置异常处理器
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler;
    }
}
