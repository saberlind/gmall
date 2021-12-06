package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/2 0:45
 */
@Data
public class SpuVo extends SpuEntity {
    private List<String> spuImages;
    private List<SpuAttrVo> baseAttrs;
    private List<SkuVo> skus;
}
