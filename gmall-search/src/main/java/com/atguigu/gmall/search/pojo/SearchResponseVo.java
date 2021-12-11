package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/8 23:31
 */
@Data
public class SearchResponseVo {

    // 品牌列表：id name logo
    private List<BrandEntity> brands;

    // 分类列表：id name
    private List<CategoryEntity> categories;

    // 规格参数列表
    private List<SearchResponseAttrVo> filters;

    // 分页参数
    private Integer pageNum;
    private Integer pageSize;
    private Long Total;

    // 当前页的商品列表
    private List<Goods> goodsList;
}
