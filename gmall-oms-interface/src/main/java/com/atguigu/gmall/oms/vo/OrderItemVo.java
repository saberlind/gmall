package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/22 19:33
 */
@Data
public class OrderItemVo {

    private Long skuId;
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs; // 销售属性
    private BigDecimal price; // 价格
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private List<ItemSaleVo> sales; // 营销信息
    private Integer weight;
}
