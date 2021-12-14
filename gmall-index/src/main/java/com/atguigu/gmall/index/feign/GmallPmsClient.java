package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/12 18:32
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
