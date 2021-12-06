package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.MemberPriceEntity;

import java.util.Map;

/**
 * 商品会员价格
 *
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-12-03 16:40:20
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

