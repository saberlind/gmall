package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author saberlin
 * @create 2021/12/17 19:52
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.KeyValueConfig> {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 指定接收参数字段顺序
     * filters:
     *   - Auth=/xxx,/yyy,/zzz
     * 可以通过不同的字段分别读取：/xxx,/yyy,/zzz
     * 在这里希望通过一个集合字段读取所有的路径
     * @return
     */
    @Override
    public List<String> shortcutFieldOrder() {
        // return Arrays.asList("value","key");  倒序
        return Arrays.asList("keys");
    }

    /**
     * 字段读取字段的结果集类型
     * 默认通过map的方式，把配置读取到不同字段
     * @return
     */
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    public AuthGatewayFilterFactory(){
        super(KeyValueConfig.class);
    }

    @Override
    public GatewayFilter apply(KeyValueConfig config) {
        // 实现GatewayFilter接口
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("自定义过滤器！" + config);

                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 1.判断当前请求路径在不在名单中，不在直接放行
                List<String> keys = config.getKeys();
                String path = request.getURI().getPath();
                if (keys.stream().allMatch(key -> path.indexOf(key) == -1)){
                    return chain.filter(exchange);
                }
                // 2.获取token信息：同步请求cookie中获取，异步请求头信息中获取
                String token = request.getHeaders().getFirst("token");
                // 头信息没有，就从cookie中尝试获取
                if (StringUtils.isEmpty(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                        token = cookies.getFirst(jwtProperties.getCookieName()).getValue();
                    }
                }

                // 3.判断token是否为空，为空直接拦截
                if (StringUtils.isEmpty(token)){
                    // 重定向到登录
                    // 303状态码标识由于请求对应的资源存在着另一个URI，应使用重定向获取请求的资源
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());

                    return response.setComplete();
                }

                try {
                    // 4.解析jwt,有异常直接拦截
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());

                    // 5.判断ip
                    String ip = map.get("ip").toString();
                    String curIp = IpUtils.getIpAddressAtGateway(request);
                    if (!StringUtils.equals(ip,curIp)){
                        // 重定向登录
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());

                        return response.setComplete();
                    }

                    // 6.传递登录信息给后续的服务，不需要再次解析jwt(因为解析jwt非常耗时，尽量少解析)
                    // 将userId转变成request对象。mutate：转变的意思
                    request.mutate().header("userId",map.get("userId").toString()).build();
                    // 将新的request对象那个转变成exchange对象
                    exchange.mutate().request(request).build();
                }catch (Exception e){
                    e.printStackTrace();
                    // 重定向登录
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }
                return chain.filter(exchange);
            }
        };
    }

    /**
     * 读取配置的内部类
     */
    @Data
    public static class KeyValueConfig{
        private List<String> keys;
//        private String key;
//        private String value;
    }
}
