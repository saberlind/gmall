package com.atguigu.gmall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@SpringBootApplication
//@RefreshScope
public class GmallGatewayApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(GmallGatewayApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
