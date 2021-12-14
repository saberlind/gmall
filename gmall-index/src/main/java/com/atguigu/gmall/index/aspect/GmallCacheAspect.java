package com.atguigu.gmall.index.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @author saberlin
 * @create 2021/12/14 0:18
 */
@Component
@Aspect
public class GmallCacheAspect {

    @Before("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void before(){
        System.out.println("前置通知。。。。。。。");
    }

}
