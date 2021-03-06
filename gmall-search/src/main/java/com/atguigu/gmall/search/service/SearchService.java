package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.*;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.amqp.core.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author saberlin
 * @create 2021/12/8 20:04
 */
@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient highLevelClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;

    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            SearchResponse response = this.highLevelClient.search(new SearchRequest(new String[]{"goods"}, buildDsl(searchParamVo)), RequestOptions.DEFAULT);
            // ?????????????????????
            SearchResponseVo responseVo = this.parseResult(response);
            //??????????????????????????????????????????
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());

            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ???????????????
     *
     * @param response
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();

        // ?????????????????????
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits().value);
        SearchHit[] hitsHits = hits.getHits();
        // ?????????????????????
        if (hitsHits != null || hitsHits.length > 0) {
            responseVo.setGoodsList(Stream.of(hitsHits).map(hitsHit -> {
                String json = hitsHit.getSourceAsString();
                // ???json??????_source???????????????Goods??????
                Goods goods = JSON.parseObject(json, Goods.class);
                // ??????????????????title?????????????????????title
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                goods.setTitle(highlightField.fragments()[0].string());
                return goods;
            }).collect(Collectors.toList()));
        }

        // ?????????????????????
        Aggregations aggregations = response.getAggregations();

        // ???????????????????????????
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        // ????????????id????????????????????????
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        // ???????????? ????????? brandEntity ??????
        if (!CollectionUtils.isEmpty(brandIdAggBuckets)) {
            responseVo.setBrands(brandIdAggBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // ????????????????????????
                Aggregations subAggs = ((Terms.Bucket) bucket).getAggregations();

                // ?????????????????????????????????????????????
                ParsedStringTerms brandNameAgg = subAggs.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    brandEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }

                // ???????????????????????????logo?????????
                ParsedStringTerms logoAgg = subAggs.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)) {
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }

        // ??????????????????????????????
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            responseVo.setCategories(categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // ??????????????????????????????
                ParsedStringTerms categroyNameAgg = ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> buckets = categroyNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    categoryEntity.setName(buckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // ????????????????????????????????????
        ParsedNested attrAgg = aggregations.get("attrAgg");
        // ?????????????????????????????????????????????Id?????????
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            responseVo.setFilters(buckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // ????????????????????????????????????
                Aggregations subAggs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms attrNameAgg = subAggs.get("attrNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    searchResponseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }
                // ?????????????????????????????????
                ParsedStringTerms attrValueAgg = subAggs.get("attrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(valueAggBuckets)) {
                    searchResponseAttrVo.setAttrValues(valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList()));
        }

        return responseVo;
    }

    /**
     * ??????DSL??????
     *
     * @param searchParamVo
     * @return
     */
    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo) {

        String keyword = searchParamVo.getKeyword();
        // ??????keyword????????????
        if (StringUtils.isBlank(keyword)) {
            // TODO: ??????????????????
            throw new RuntimeException("???????????????????????? ???????????????");
        }

        // ?????????????????????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 1.???????????????????????????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1. ????????????????????????
        sourceBuilder.query(boolQueryBuilder);

        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        // 1.2. ??????????????????
        // 1.2.1 ??????????????????
        List<Long> brandIds = searchParamVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandIds)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandIds));
        }
        // 1.2.2 ??????????????????
        List<Long> categoryIds = searchParamVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryIds)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryIds));
        }
        // 1.2.3 ??????????????????
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        // ??????????????????????????????????????????????????????????????????
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
        }
        // 1.2.4 ??????????????????
        Boolean store = searchParamVo.getStore();
        if (store != null) {
            // ?????????????????????????????????
            TermQueryBuilder termQuery = QueryBuilders.termQuery("store", store);
            boolQueryBuilder.filter(termQuery);
        }
        // 1.2.5 ????????????????????????
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                // ["4:8G-12G,"5:128G-256G,"8:??????"]
                String[] attrs = StringUtils.split(prop, ":");
                // ??????????????????????????????????????????????????????????????????
                if (attrs != null && attrs.length == 2 && NumberUtils.isCreatable(attrs[0])) {
                    // ????????????????????????????????????????????????????????????????????????
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                    // ????????????id??????
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    // ?????????????????????
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));

                }
            });
        }
        // 2. ????????????
        Integer sort = searchParamVo.getSort();
        switch (sort) {
            case 1:
                sourceBuilder.sort("price", SortOrder.DESC);
                break;
            case 2:
                sourceBuilder.sort("price", SortOrder.ASC);
                break;
            case 3:
                sourceBuilder.sort("sales", SortOrder.DESC);
                break;
            case 4:
                sourceBuilder.sort("createTime", SortOrder.DESC);
                break;
            default:
                sourceBuilder.sort("_score", SortOrder.DESC);
                break;
        }
        // 3. ????????????
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        // 4. ????????????
        sourceBuilder.highlighter(new HighlightBuilder().field("title")
                .preTags("<font style='color:red;'>")
                .postTags("</font>"));
        // 5. ????????????
        // 5.1 ????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        // 5.2 ????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        // 5.3 ??????????????????
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));
        // 6.???????????????
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "subtitle", "defaultImage", "price"}, null);

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

    /**
     * ????????????
     */
    public void createIndex(Long spuId, Channel channel, Message message) throws IOException {

        // 1.??????spuId??????spu
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if (spuEntity == null) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        // 2.???????????????spu??????sku
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skus = skuResponseVo.getData();
        if (!CollectionUtils.isEmpty(skus)){
            // 4.????????????id????????????
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            // 5.???????????????id????????????
            ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
            CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
            // 7.??????spuId?????????id?????????????????????????????????????????????
            ResponseVo<List<SpuAttrValueEntity>> baseAttrResponseVo = this.pmsClient.querySpuAttrValueByCidAndSpuId(spuEntity.getCategoryId(), spuId);
            List<SpuAttrValueEntity> baseAttrEntities = baseAttrResponseVo.getData();
            // ???sku???????????????goods??????
            this.goodsRepository.saveAll(skus.stream().map(skuEntity -> {
                Goods goods = new Goods();

                // ??????sku????????????
                goods.setSkuId(skuEntity.getId());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubtitle(skuEntity.getSubtitle());
                goods.setPrice(skuEntity.getPrice());
                goods.setDefaultImage(skuEntity.getDefaultImage());

                // ??????????????????
                goods.setCreateTime(spuEntity.getCreateTime());
                // 3.??????skuId ????????????????????? ?????????
                ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)){
                    goods.setSale(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }
                // ????????????????????????
                if (brandEntity != null){
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }
                //????????????????????????
                if (categoryEntity != null){
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }
                List<SearchAttrValueVo> attrValueVos = new ArrayList<>();

                // 6.??????skuId???cid???????????????????????????????????????
                ResponseVo<List<SkuAttrValueEntity>> skuAttrValueVo = this.pmsClient.querySkuAttrValueByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                // ????????????????????????????????????  ??????  ????????? SearchAttrValueVo ??????
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueVo.getData();
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                // ????????????????????????????????????  ?????? ????????? SearchAttrValueVo??????
                if (!CollectionUtils.isEmpty(baseAttrEntities)){
                    attrValueVos.addAll(baseAttrEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                //???????????????????????????????????????
                goods.setSearchAttrs(attrValueVos);
                return goods;
            }).collect(Collectors.toList()));
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
