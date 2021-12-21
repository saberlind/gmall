package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;

@EnableConfigurationProperties(JwtProperties.class)
@Component
//@Data
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    //private UserInfo userInfo;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获取cookie中的内容
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        // userkey不管有没有登录，都会有
        String userKey = CookieUtils.getCookieValue(request, this.properties.getUserKey());
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, this.properties.getUserKey(), userKey, this.properties.getExpire());
        }

        // 解析token获取userId
        if (StringUtils.isBlank(token)){
            THREAD_LOCAL.set(new UserInfo(null, userKey));
            return true;
        }

        Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
        Long userId = Long.valueOf(map.get("userId").toString());
        THREAD_LOCAL.set(new UserInfo(userId, userKey));

        // 返回值为false，则handler方法不再执行。
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 一定要手动释放ThreadLocal中的资源，因为我们使用的是线程池，线程没有结束，可能导致内存泄漏，进而导致OOM
        THREAD_LOCAL.remove();
    }
}
