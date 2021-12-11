package com.atguigu.gmall.oms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableFeignClients
@MapperScan("com.atguigu.gmall.oms.mapper")
@RefreshScope
@EnableSwagger2
@SpringBootApplication
public class GmallOmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(GmallOmsApplication.class, args);
    }
}
