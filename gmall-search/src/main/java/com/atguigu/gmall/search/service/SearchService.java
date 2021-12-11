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
            // 解析搜索结果集
            SearchResponseVo responseVo = this.parseResult(response);
            //分页参数只能从搜索条件中获取
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());

            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析结果集
     *
     * @param response
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();

        // 解析搜索结果集
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits().value);
        SearchHit[] hitsHits = hits.getHits();
        // 获取当前页数据
        if (hitsHits != null || hitsHits.length > 0) {
            responseVo.setGoodsList(Stream.of(hitsHits).map(hitsHit -> {
                String json = hitsHit.getSourceAsString();
                // 把json类型_source反序列化为Goods对象
                Goods goods = JSON.parseObject(json, Goods.class);
                // 解析出高亮的title，替换掉普通的title
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                goods.setTitle(highlightField.fragments()[0].string());
                return goods;
            }).collect(Collectors.toList()));
        }

        // 解析聚合结果集
        Aggregations aggregations = response.getAggregations();

        // 获取品牌聚合结果集
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        // 获取品牌id聚合结果集中的桶
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        // 把桶集合 转化成 brandEntity 集合
        if (!CollectionUtils.isEmpty(brandIdAggBuckets)) {
            responseVo.setBrands(brandIdAggBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // 获取桶中的子聚合
                Aggregations subAggs = ((Terms.Bucket) bucket).getAggregations();

                // 获取子聚合中的品牌名称的子聚合
                ParsedStringTerms brandNameAgg = subAggs.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    brandEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }

                // 获取子聚合中的品牌logo子聚合
                ParsedStringTerms logoAgg = subAggs.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)) {
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }

        // 解析分类的聚合结果集
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            responseVo.setCategories(categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 获取分类名称的子聚合
                ParsedStringTerms categroyNameAgg = ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> buckets = categroyNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    categoryEntity.setName(buckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // 解析规格参数的聚合结果集
        ParsedNested attrAgg = aggregations.get("attrAgg");
        // 获取嵌套聚合结果集中的规格参数Id子聚合
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            responseVo.setFilters(buckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 获取规格参数名称的子聚合
                Aggregations subAggs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms attrNameAgg = subAggs.get("attrNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)) {
                    searchResponseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }
                // 获取规格参数值的子聚合
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
     * 构建DSL语句
     *
     * @param searchParamVo
     * @return
     */
    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo) {

        String keyword = searchParamVo.getKeyword();
        // 判断keyword是否为空
        if (StringUtils.isBlank(keyword)) {
            // TODO: 返回广告商品
            throw new RuntimeException("抱歉，没有找到与 相关的商品");
        }

        // 搜索条件构建器
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 1.构建查询及过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1. 构建匹配查询条件
        sourceBuilder.query(boolQueryBuilder);

        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        // 1.2. 构建过滤条件
        // 1.2.1 构建品牌过滤
        List<Long> brandIds = searchParamVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandIds)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandIds));
        }
        // 1.2.2 构建分类过滤
        List<Long> categoryIds = searchParamVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryIds)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryIds));
        }
        // 1.2.3 构建价格区间
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        // 如果任何一个价格不为空，都要有价格范围的过滤
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
        // 1.2.4 构建是否有货
        Boolean store = searchParamVo.getStore();
        if (store != null) {
            // 正常情况下只显示有货的
            TermQueryBuilder termQuery = QueryBuilders.termQuery("store", store);
            boolQueryBuilder.filter(termQuery);
        }
        // 1.2.5 构建规格参数过滤
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                // ["4:8G-12G,"5:128G-256G,"8:麒麟"]
                String[] attrs = StringUtils.split(prop, ":");
                // 字符串切割后必须为两份，并且第一份必须为数字
                if (attrs != null && attrs.length == 2 && NumberUtils.isCreatable(attrs[0])) {
                    // 如果规格参数的过滤条件合法，添加规格参数嵌套过滤
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                    // 规格参数id过滤
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    // 规格参数值过滤
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));

                }
            });
        }
        // 2. 构建排序
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
        // 3. 构建分页
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        // 4. 构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title")
                .preTags("<font style='color:red;'>")
                .postTags("</font>"));
        // 5. 构建聚合
        // 5.1 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        // 5.2 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        // 5.3 规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));
        // 6.结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "subtitle", "defaultImage", "price"}, null);

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

    /**
     * 创建索引
     */
    public void createIndex(Long spuId, Channel channel, Message message) throws IOException {

        // 1.根据spuId查询spu
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if (spuEntity == null) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        // 2.查询每一个spu下的sku
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skus = skuResponseVo.getData();
        if (!CollectionUtils.isEmpty(skus)){
            // 4.根据品牌id查询品牌
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            // 5.根据分类的id查询分类
            ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
            CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
            // 7.根据spuId和分类id查询基本类型的检索规格参数及值
            ResponseVo<List<SpuAttrValueEntity>> baseAttrResponseVo = this.pmsClient.querySpuAttrValueByCidAndSpuId(spuEntity.getCategoryId(), spuId);
            List<SpuAttrValueEntity> baseAttrEntities = baseAttrResponseVo.getData();
            // 把sku集合转化成goods集合
            this.goodsRepository.saveAll(skus.stream().map(skuEntity -> {
                Goods goods = new Goods();

                // 设置sku相关参数
                goods.setSkuId(skuEntity.getId());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubtitle(skuEntity.getSubtitle());
                goods.setPrice(skuEntity.getPrice());
                goods.setDefaultImage(skuEntity.getDefaultImage());

                // 设置创建时间
                goods.setCreateTime(spuEntity.getCreateTime());
                // 3.根据skuId 查询是否有库存 、销量
                ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)){
                    goods.setSale(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }
                // 设置品牌相关参数
                if (brandEntity != null){
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }
                //设置分类相关参数
                if (categoryEntity != null){
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }
                List<SearchAttrValueVo> attrValueVos = new ArrayList<>();

                // 6.根据skuId和cid查询检索类型的销售属性和值
                ResponseVo<List<SkuAttrValueEntity>> skuAttrValueVo = this.pmsClient.querySkuAttrValueByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                // 把检索类型的销售属性和值  集合  转化成 SearchAttrValueVo 集合
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueVo.getData();
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                // 把检索类型的基本属性和值  集合 转化成 SearchAttrValueVo集合
                if (!CollectionUtils.isEmpty(baseAttrEntities)){
                    attrValueVos.addAll(baseAttrEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                //设置检索类型的规格参数及值
                goods.setSearchAttrs(attrValueVos);
                return goods;
            }).collect(Collectors.toList()));
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
