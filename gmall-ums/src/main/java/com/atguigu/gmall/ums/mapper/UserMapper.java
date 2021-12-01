package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-11-30 00:49:35
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
