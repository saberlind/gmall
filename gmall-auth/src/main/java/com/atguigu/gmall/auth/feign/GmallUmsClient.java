package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/17 18:43
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
