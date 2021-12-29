package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    // 已选中的收获地址
    private UserAddressEntity address;

    // 购买积分
    private Integer bounds;

    // 配送方式
    private String deliveryCompany;

    // 送货清单
    private List<OrderItemVo> items;

    // 防重的唯一标识
    private String orderToken;

    // 支付方式
    private Integer payType;

    // 验价所需的总价格
    private BigDecimal totalPrice;

}
