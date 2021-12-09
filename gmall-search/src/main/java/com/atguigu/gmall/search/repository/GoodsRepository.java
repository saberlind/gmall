package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author saberlin
 * @create 2021/12/7 23:21
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
