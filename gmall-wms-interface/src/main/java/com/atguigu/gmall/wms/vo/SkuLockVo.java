package com.atguigu.gmall.wms.vo;

import lombok.Data;

/**
 * @author saberlin
 * @create 2021/12/23 9:56
 */
@Data
public class SkuLockVo {

    private Long skuId;
    private Integer count;
    private Boolean lock; //是否锁定成功
    private Long wareId; //锁定成功的情况下，记录锁定成功的仓库id，以方便将来解锁  或者  减库存
}
