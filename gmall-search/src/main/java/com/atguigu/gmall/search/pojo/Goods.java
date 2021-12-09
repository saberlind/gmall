package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/6 20:48
 */
@Document(indexName = "goods",shards = 3,replicas = 2)
@Data
public class Goods {

    @Id
    private Long skuId;
    @Field(type= FieldType.Text,analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword,index = false)
    private String subtitle;
    @Field(type = FieldType.Double)
    private BigDecimal price;
    @Field(type = FieldType.Keyword,index = false)
    private String defaultImage;

    // 排序及过滤
    @Field(type = FieldType.Long)
    private Long sale = 0L;
    @Field(type = FieldType.Date,format = DateFormat.date_time)
    private Date createTime;
    @Field(type = FieldType.Boolean)
    private Boolean store = false;

    // 品牌过滤
    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Keyword)
    private String logo;

    // 分类过滤
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;

    // 规格参数过滤
    @Field(type = FieldType.Nested)
    private List<SearchAttrValueVo> searchAttrs;
}
