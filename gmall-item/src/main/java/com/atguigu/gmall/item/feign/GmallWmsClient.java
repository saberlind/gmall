package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/15 11:48
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
