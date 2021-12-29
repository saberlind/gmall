package com.atguigu.gmall.order.interceptor;

import com.atguigu.gmall.order.vo.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
//@Data
public class LoginInterceptor implements HandlerInterceptor {

    //private UserInfo userInfo;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("userId");
        String username = request.getHeader("username");

        Long id = Long.valueOf(userId);

        THREAD_LOCAL.set(new UserInfo(id,null,username));
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
