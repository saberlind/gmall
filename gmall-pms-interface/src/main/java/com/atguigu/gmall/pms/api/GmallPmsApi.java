package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/7 22:17
 */

public interface GmallPmsApi {

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId") Long parentId);

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @PostMapping("pms/spu/page")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/brand/{id}")
    ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/spuattrvalue/category/{cid}")
    ResponseVo<List<SpuAttrValueEntity>> querySpuAttrValueByCidAndSpuId(
            @PathVariable("cid")Long cid,
            @RequestParam("spuId") Long spuId
    );

    @GetMapping("pms/skuattrvalue/category/{cid}")
    ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValueByCidAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("skuId") Long skuId
    );

    @GetMapping("pms/category/sub/all/{pid}")
    ResponseVo<List<CategoryEntity>> queryLv23CategoriesByPid(@PathVariable("pid")Long pid);
}
