package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/15 11:48
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
