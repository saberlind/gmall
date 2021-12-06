package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.HomeSubjectEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页专题表【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
 * 
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-12-03 16:40:20
 */
@Mapper
public interface HomeSubjectMapper extends BaseMapper<HomeSubjectEntity> {
	
}
