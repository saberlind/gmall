package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SkuEntity;

import java.util.List;
import java.util.Map;

/**
 * sku信息
 *
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-11-29 20:01:44
 */
public interface SkuService extends IService<SkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuEntity> querySkuBySpuId(Long spuId);
}

