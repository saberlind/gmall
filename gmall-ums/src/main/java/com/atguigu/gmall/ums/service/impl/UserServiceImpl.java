package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.exception.UserException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );
        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1: queryWrapper.eq("username",data);break;
            case 2: queryWrapper.eq("phone",data); break;
            case 3: queryWrapper.eq("email",data); break;
            default:
                return null;
        }
        return this.count(queryWrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        // TODO 1.校验短信验证码，手机号获取redis中的验证码
        // 2.生成盐
        String uuid = UUID.randomUUID().toString();
        String salt = StringUtils.substring(uuid, 0, 6);
        userEntity.setSalt(salt);
        // 3.对密码进行加盐
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() +salt));
        // 4.新增
        userEntity.setLevelId(1L);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setCreateTime(new Date());
        userEntity.setStatus(1);
        this.save(userEntity);

        // TODO 5.删除短信验证码
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        UserEntity userEntity = this.getOne(new QueryWrapper<UserEntity>()
                .eq("username", loginName)
                .or()
                .eq("phone", loginName)
                .or()
                .eq("email", loginName));
        if (userEntity == null){
            throw new UserException("账号输入有误！！！");
        }
        // 对密码加盐加密，并和数据库的密码比较
        String salt = userEntity.getSalt();
        String loginPassword = password + salt;
        String Md5LoginPassword = DigestUtils.md5Hex(loginPassword);
        if (!StringUtils.equals(userEntity.getPassword(),Md5LoginPassword)){
            throw new UserException("密码输入错误");
        }
        // 4.返回用户信息
        return userEntity;
    }
}