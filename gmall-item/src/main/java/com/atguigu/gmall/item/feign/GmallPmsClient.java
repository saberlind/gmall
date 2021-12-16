package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author saberlin
 * @create 2021/12/14 23:48
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
