package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {


    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySpuAttrValueByCidAndSpuId(Long cid, Long skuId) {
        List<AttrEntity> attrEntities = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id",skuId).in("attr_id",attrIds));

    }

    @Override
    public List<SaleAttrValueVo> querySkuAttrsBySpuId(Long spuId) {
        // 查询spu下的所有sku
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new LambdaQueryWrapper<SkuEntity>().eq(SkuEntity::getSpuId, spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        // 获取skuId集合
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        //根据skuIds查询销售属性集合
        List<SkuAttrValueEntity> skuAttrValueEntities = this.list(new LambdaQueryWrapper<SkuAttrValueEntity>().in(SkuAttrValueEntity::getSkuId, skuIds));
        if (CollectionUtils.isEmpty(skuAttrValueEntities)){
            return null;
        }

        // 把销售属性集合转化成SaleAttrValueVo集合
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        // 以attrId为key，以attrId相同的数据作为value
        Map<Long,List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        map.forEach((attrId,skuAttrValues) ->{
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            // 只要有这个分组，skuAttrValues至少会有一个元素
            saleAttrValueVo.setAttrName(skuAttrValues.get(0).getAttrName());
            Set<String> attrValues = skuAttrValues.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet());
            saleAttrValueVo.setAttrValues(attrValues);
            saleAttrValueVos.add(saleAttrValueVo);
        });
        return saleAttrValueVos;
    }

    @Override
    public String queryMappingBySpuId(Long spuId) {
        // 根据spuId查询spu下所有的sku
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new LambdaQueryWrapper<SkuEntity>().eq(SkuEntity::getSpuId, spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        // 获取skuId集合
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        // 查询到映射关系
        List<Map<String,Object>> maps = this.attrValueMapper.queryMappingBySkuIds(skuIds);
        if (CollectionUtils.isEmpty(maps)){
            return null;
        }

        // 把上述集合转化成{'白天白,8G,512G': 10, '白天白,12G,256G': 11}
        Map<String, Long> mappingMap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));
        return JSON.toJSONString(mappingMap);
    }
}