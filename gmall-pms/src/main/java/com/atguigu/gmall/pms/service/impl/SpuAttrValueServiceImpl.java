package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("spuAttrValueService")
public class SpuAttrValueServiceImpl extends ServiceImpl<SpuAttrValueMapper, SpuAttrValueEntity> implements SpuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SpuAttrValueEntity> querySpuAttrValueByCidAndSpuId(Long cid, Long spuId) {
        // 根据cid 获取检索类型的属性
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        // 获取规格参数id集合
        List<Long> attrId =  attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
        // 通过规格参数id获取规格参数及值
        List<SpuAttrValueEntity> spuAttrValueEntities = this.list(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrId));
        return spuAttrValueEntities;
    }

}