package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * sku销售属性&值
 *
 * @author saberlin
 * @email 1513692145@qq.com
 * @date 2021-11-29 20:01:44
 */
@Api(tags = "sku销售属性&值 管理")
@RestController
@RequestMapping("pms/skuattrvalue")
public class SkuAttrValueController {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @GetMapping("mapping/{spuId}")
    public ResponseVo<String> queryMappingBySpuId(@PathVariable("spuId")Long spuId){
        String json = this.skuAttrValueService.queryMappingBySpuId(spuId);
        return ResponseVo.ok(json);
    }

    @GetMapping("sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValuesBySkuId(@PathVariable("skuId")Long skuId){
        List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueService.list(new LambdaQueryWrapper<SkuAttrValueEntity>().eq(SkuAttrValueEntity::getSkuId, skuId));
        return ResponseVo.ok(skuAttrValueEntities);
    }

    @GetMapping("spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValuesBySpuId(@PathVariable("spuId")Long spuId){
        List<SaleAttrValueVo> skuAttrValueEntities = skuAttrValueService.querySkuAttrsBySpuId(spuId);
        return ResponseVo.ok(skuAttrValueEntities);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuAttrValueByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = skuAttrValueService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    @GetMapping("category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValueByCidAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("skuId") Long skuId
    ){
        List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueService.querySpuAttrValueByCidAndSpuId(cid,skuId);
        return ResponseVo.ok(skuAttrValueEntities);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuAttrValueEntity> querySkuAttrValueById(@PathVariable("id") Long id){
		SkuAttrValueEntity skuAttrValue = skuAttrValueService.getById(id);

        return ResponseVo.ok(skuAttrValue);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.save(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.updateById(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		skuAttrValueService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
