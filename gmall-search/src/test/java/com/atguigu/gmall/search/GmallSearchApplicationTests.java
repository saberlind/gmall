package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.util.CollectionUtils;
import com.atguigu.gmall.search.repository.GoodsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GoodsRepository goodsRepository;

    Integer pageNum = 1;
    Integer pageSize = 100;

    @Test
    void contextLoads() {

        IndexOperations indexOps = this.restTemplate.indexOps(Goods.class);
        if (!indexOps.exists()) {
            indexOps.create();
        }
        indexOps.putMapping(indexOps.createMapping());

        do {
            // 1. 分页查询spu
            ResponseVo<List<SpuEntity>> responseVo = this.gmallPmsClient.querySpuByPageJson(new PageParamVo(pageNum, pageSize, null));
            List<SpuEntity> spuEntities = responseVo.getData();
            // 判断当前页的spu是否为空
            if (CollectionUtils.isEmpty(spuEntities)) {
                return;
            }
            // 遍历spu
            spuEntities.forEach(spu -> {
                // 2.查询每一个spu下的sku
                ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsClient.querySkuBySpuId(spu.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();

                // 4.根据brandId查询品牌
                ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsClient.queryBrandById(spu.getBrandId());
                BrandEntity brandEntity = brandEntityResponseVo.getData();

                // 5.根据分类id查询分类
                ResponseVo<CategoryEntity> categoryEntityResponseVo = this.gmallPmsClient.queryCategoryById(spu.getCategoryId());
                CategoryEntity categoryEntity = categoryEntityResponseVo.getData();

                // 7.根据cid和spuId查询检索类型的基规格参数和值
                ResponseVo<List<SpuAttrValueEntity>> listResponseVo = this.gmallPmsClient.querySpuAttrValueByCidAndSpuId(spu.getCategoryId(), spu.getId());
                List<SpuAttrValueEntity> spuAttrValueEntities = listResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    // 将sku转化为goods
                    this.goodsRepository.saveAll(skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubtitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice());
                        goods.setDefaultImage(skuEntity.getDefaultImage());

                        // 设置创建时间
                        goods.setCreateTime(spu.getCreateTime());

                        // 3.根据skuId查询销量和库存
                        ResponseVo<List<WareSkuEntity>> wareResponVo = this.gmallWmsClient.queryWareBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareResponVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            goods.setSale(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                        }
                        // 设置品牌相关参数
                        if (brandEntity!=null){
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        // 设置分类相关参数
                        if (categoryEntity!=null){
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setBrandName(categoryEntity.getName());
                        }

                        List<SearchAttrValueVo> attrValueVos = new ArrayList<>();
                        // 6.根据cid和skuId查询检索类型的销量属性及值
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponses = this.gmallPmsClient.querySkuAttrValueByCidAndSkuId(categoryEntity.getId(), skuEntity.getId());

                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponses.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        // 设置检索类型的规格参数及值
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        goods.setSearchAttrs(attrValueVos);
                        return goods;
                    }).collect(Collectors.toList()));
                }
            });
            pageNum++;
            pageSize = spuEntities.size();
        } while (pageSize == 100);
    }
}
