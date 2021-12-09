package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/7 23:54
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
