package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

/**
 * spu信息
 *
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-11-29 20:01:44
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo querySpuInfo(PageParamVo pageParamVo, Long cid);

    void bigSave(SpuVo spu) throws FileNotFoundException;

    void saveSku(SpuVo spuVo);

    void saveBaseAttr(SpuVo spuVo, Long spuId);

    void saveSpuDesc(SpuVo spuVo, Long spuId);

    Long saveSpu(SpuVo spuVo);
}

