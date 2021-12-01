package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-11-29 21:03:10
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
