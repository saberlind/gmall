package com.atguigu.gmall.sms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/3 11:42
 */
@Data
public class SkuSaleVo {

    private Long skuId;
    /**
     * 积分成长相关字段
     */
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    /**
     * 优惠生效情况[1111(四个状态位，从右到左)]
     * 0 - 无优惠，成长积分是否赠送;
     * 1 - 无优惠，购物积分是否赠送;
     * 2 - 有优惠，成长积分是否赠送;
     * 3 - 有优惠，购物积分是否赠送【状态位0：不赠送，1：赠送】]
     */
    List<Integer> work;
    /**
     * 打折优惠相关字段
     */
    private Integer fullCount;//满多少件
    private BigDecimal discount;
    /**
     * 是否叠加其他优惠[0-不可叠加，1-可叠加]
     */
    private Integer ladderAddOther;
    /*
     * 满减优惠相关字段
     * */
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;
}
