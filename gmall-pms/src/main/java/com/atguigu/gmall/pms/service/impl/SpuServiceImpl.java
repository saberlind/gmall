package com.atguigu.gmall.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.service.SpuService;
import sun.swing.StringUIClientPropertyKey;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );
        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuInfo(PageParamVo pageParamVo, Long cid) {
        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();
        // 如果分类id不为0，就根据分类id查询
        if (cid != 0){
            queryWrapper.eq("category_id",cid);
        }
        // 如果用户输入了检索条件，要根据检索条件查
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)){
            queryWrapper.and(t->t.like("name",key).or().like("id",key));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(),queryWrapper));
    }
}