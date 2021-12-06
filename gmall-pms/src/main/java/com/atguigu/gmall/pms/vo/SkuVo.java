package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/2 1:05
 */
@Data
public class SkuVo extends SkuEntity {

   /**
    * 积分成长相关字段
    */
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    List<Integer> work;
   /**
    * 打折优惠相关字段
    */
    private Integer fullCount;//满多少件
    private BigDecimal discount;
    private Integer ladderAddOther;
    /*
     * 满减优惠相关字段
     * */
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;
    /*
    * 图片列表
    * */
    private List<String> images;
    /*
    * 销售属性
    * */
    private List<SkuAttrValueEntity> saleAttrs;
}
