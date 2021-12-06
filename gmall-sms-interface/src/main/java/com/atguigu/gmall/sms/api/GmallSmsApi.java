package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author saberlin
 * @create 2021/12/3 18:54
 */
public interface GmallSmsApi {
    @PostMapping("sms/skubounds/skusale/save")
    public ResponseVo skuSaleSave(@RequestBody SkuSaleVo skuSaleVo);
}
