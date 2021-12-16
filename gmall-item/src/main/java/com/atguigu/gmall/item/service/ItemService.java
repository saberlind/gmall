package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.ItemException;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author saberlin
 * @create 2021/12/14 23:49
 */
@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private TemplateEngine templateEngine;

    public ItemVo loadData(Long skuId) {

        ItemVo itemVo = new ItemVo();

        // 1.根据skuId查询sku的信息
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(()->{
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null){
                throw new ItemException("当前skuId对应的商品不存在！");
            }
            //BeanUtils.copyProperties(skuEntity,itemVo);
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubtitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;
        },executorService);

        //2. 根据cid查询一二三级分类
        CompletableFuture<Void> catesFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> cateresponseVo = this.pmsClient.queryLv123categoriesById(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = cateresponseVo.getData();
            itemVo.setCategories(categoryEntities);
        },executorService);

        //3.根据brandId查询品牌
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity!=null){
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },executorService);

        //4.根据spuId查询spu
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity!=null){
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        },executorService);

        //5.根据skuId查询图片列表
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(()->{
            ResponseVo<List<SkuImagesEntity>> skuImagesBySkuId = this.pmsClient.querySkuImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = skuImagesBySkuId.getData();
            itemVo.setImages(skuImagesEntities);
        },executorService);

        //6.根据skuId查询优惠信息
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() ->{
            ResponseVo<List<ItemSaleVo>> salesBySkuId = this.smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesBySkuId.getData();
            itemVo.setSales(itemSaleVos);
        },executorService);

        //7.根据skuId查询库存列表
        CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(()->{
            ResponseVo<List<WareSkuEntity>> queryWareBySkuId = this.wmsClient.queryWareBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = queryWareBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() -wareSkuEntity.getStockLocked() > 0));
            }
        },executorService);

        // 8.根据spuId查询spu下所有销售属性列表
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrValuesBySpuId = this.pmsClient.querySaleAttrValuesBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrValuesBySpuId.getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        },executorService);

        //9.根据skuId查询当前sku的销售属性
        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(()->{
            ResponseVo<List<SkuAttrValueEntity>> saleAttrValuesBySkuId = this.pmsClient.querySaleAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValuesBySkuId.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId,SkuAttrValueEntity::getAttrValue)));
            }
        },executorService);

        //10.根据spuId查询spu下所有销售属性组合与skuId的映射关系
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> stringResponseVo = this.pmsClient.queryMappingBySpuId(skuEntity.getSpuId());
            String json = stringResponseVo.getData();
            itemVo.setSkuJsons(json);
        },executorService);

        //11.根据spuId查询描述信息
        CompletableFuture<Void> spuDescFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity descEntity = spuDescEntityResponseVo.getData();
            if (descEntity!=null){
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(descEntity.getDecript(), ",")));
            }
        },executorService);

        //12.根据cid spuId skuId 查询组及组下的规格参数和值
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupWithAttrAndValueByCidAndSpuIdAndSkuId = this.pmsClient.queryGroupWithAttrAndValueByCidAndSpuIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> groupResponseVo = groupWithAttrAndValueByCidAndSpuIdAndSkuId.getData();
            itemVo.setGroups(groupResponseVo);
        },executorService);

        CompletableFuture.allOf(catesFuture, brandFuture, spuFuture, imagesFuture, salesFuture, wareFuture,
                saleAttrsFuture, saleAttrFuture, mappingFuture, spuDescFuture, groupFuture).join();

        executorService.execute(()->{
            this.generateHtml(itemVo);
        });
        return itemVo;
    }

    private void generateHtml(ItemVo itemVo){
        executorService.execute(()->{
            try (PrintWriter printWriter = new PrintWriter("E:\\atguigu_Java\\generator\\" + itemVo.getSkuId() + ".html")) {
                // Thymeleaf上下文对象，通过它给模板传递数据
                Context context = new Context();
                context.setVariable("itemVo",itemVo);
                // 生成静态页面方法 1-模板名称  2-上下文对象  3.输出流
                templateEngine.process("item",context,printWriter);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
