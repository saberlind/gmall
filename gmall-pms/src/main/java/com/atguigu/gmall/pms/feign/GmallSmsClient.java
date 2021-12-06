package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/3 18:13
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
