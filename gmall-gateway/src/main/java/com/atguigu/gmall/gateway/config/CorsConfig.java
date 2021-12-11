package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @author saberlin
 * @create 2021/11/30 16:39
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){
        // 初始化一个配置对象
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许跨域访问的域名。*-允许所有，建议不要写*,原因：1-不够安全  2-不能携带cookie
        configuration.addAllowedOrigin("http://api.saberlind.club");
        configuration.addAllowedOrigin("http://man.saberlind.club");
        configuration.addAllowedOrigin("http://api.gmall.com");
        // 允许跨域访问的方法
        configuration.addAllowedMethod("*");
        // 允许携带的请求头消息
        configuration.addAllowedHeader("*");
        // 是否允许携带cookie
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**",configuration);
        return new CorsWebFilter(corsConfigurationSource);
    }
}
