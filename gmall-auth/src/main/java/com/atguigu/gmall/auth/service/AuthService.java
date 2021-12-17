package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.AuthException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


/**
 * @author saberlin
 * @create 2021/12/17 18:45
 */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;

    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response){
        ResponseVo<UserEntity> responseVo = this.umsClient.queryUser(loginName, password);
        UserEntity userEntity = responseVo.getData();
        if (userEntity == null){
            throw new AuthException("您输入用户名或密码有误!!!");
        }

        // 生成一个载荷
        Map<String,Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("username",userEntity.getUsername());
        // 为了防止盗用，添加登录时的ip地址
        map.put("ip", IpUtils.getIpAddressAtService(request));

        try {
            // 生成jwt类型的token
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());
            // 将jwt放入cookie
            CookieUtils.setCookie(request,response,this.jwtProperties.getCookieName(),token,this.jwtProperties.getExpire() * 60);
            // 昵称回显
            CookieUtils.setCookie(request,response,this.jwtProperties.getUnick(),userEntity.getNickname(),this.jwtProperties.getExpire() * 60);
        }catch (Exception e){
            e.printStackTrace();
            throw  new AuthException("用户名或者密码错误");
        }
    }
}
