package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author saberlin
 * @create 2021/12/14 23:12
 */
@Data
public class ItemVo {

    // 一 二 三  级分类
    private List<CategoryEntity> categories;

    // 品牌相关字段
    private Long brandId;
    private String brandName;

    // spu相关字段
    private Long spuId;
    private String spuName;

    // sku相关字段
    private Long skuId;
    private String skuName;
    private String defaultImage;
    private String title;
    private String subtitle;
    private BigDecimal price;
    private Integer weight;

    // sku图片列表
    private List<SkuImagesEntity> images;

    // 营销信息(优惠)
    private List<ItemSaleVo> sales;

    // 是否有货
    private Boolean store;

    // spu下所有sku的销售属性集合
    // [{attrId:3,attrName:机身颜色,attrValue:[‘暗夜黑’,'白色']}]
    // [{attrId:4,attrName:机身内存,attrValue:[‘8G’,'12G']}]
    // [{attrId:5,attrName:机身存储,attrValue:[‘128G’,'512G']}]
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性
    // {3:'暗夜黑',4:'12G',5:'512G'}
    private Map<Long,String> saleAttr;

    // spu下所有销售属性组合与skuId的映射关系
    // {'白天白,8G,512G':10,'暗夜黑,12G,128G':11}
    private String skuJsons;

    // spu描述信息
    private List<String> spuImages;

    // 规格参数分组
    private List<ItemGroupVo> groups;
}
