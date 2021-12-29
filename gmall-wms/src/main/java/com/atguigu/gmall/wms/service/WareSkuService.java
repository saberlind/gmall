package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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

    List<SkuLockVo> checkLock(List<SkuLockVo> lockVos, String orderToken);
}

