package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.wms.entity.WareEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-11-30 00:54:24
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<WareSkuEntity> queryWareBySkuId(Long skuId);
}

