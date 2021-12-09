package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/7 22:19
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
