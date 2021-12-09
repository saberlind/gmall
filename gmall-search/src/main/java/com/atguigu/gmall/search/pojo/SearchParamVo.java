package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/8 19:35
 *
 * 接收页面传递过来的检索参数
 * search.gmall.com/search?keyword=手机&brandId=1,2,3&categoryId=225
 * &props=4:8G-12G&props=5:128G-256G
 * &priceFrom=1000&priceTo=5000&store=true&sort=1&pageNum=1
 */
@Data
public class SearchParamVo {

    //搜索关键字
    private String keyword;

    //品牌过滤条件
    private List<Long> brandId;

    // 分类过滤条件
    private List<Long> categoryId;

    // 规格参数过滤条件：["4:8G-12G,"5:128G-256G,"8:麒麟"]
    private List<String> props;

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

    // 是否有货过滤
    private Boolean store;

    // 排序字段: 0-得分排序 1-价格降序 2-价格升序 3-销量排序 4-新品降序
    private Integer sort = 0;

    // 分页参数
    private Integer pageNum = 1;
    private final Integer pageSize = 20;
}
