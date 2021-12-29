package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author saberlin
 * @create 2021/12/22 19:40
 */
public interface GmallOmsApi {

    @PostMapping("oms/order/create/{userId}")
    public ResponseVo saveOrder(@RequestBody OrderSubmitVo submitVo, @PathVariable("userId")Long userId);
}
