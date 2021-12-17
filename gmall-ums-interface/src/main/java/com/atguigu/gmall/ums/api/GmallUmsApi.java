package com.atguigu.gmall.ums.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author saberlin
 * @create 2021/12/16 15:15
 */
public interface GmallUmsApi {

    @GetMapping("ums/user/query")
    public ResponseVo<UserEntity> queryUser(
            @RequestParam("loginName")String loginName,
            @RequestParam("password")String password
    );

    @GetMapping("ums/user/check/{data}/{type}")
    public ResponseVo<Boolean> checkData(@PathVariable("data")String data, @PathVariable("type")Integer type);

    @PostMapping("ums/user/register")
    public ResponseVo register(UserEntity userEntity, @RequestParam(value = "code",required = false)String code);
}
