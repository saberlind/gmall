package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.*;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient gmallSmsClient;

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
        if (cid != 0) {
            queryWrapper.eq("category_id", cid);
        }
        // 如果用户输入了检索条件，要根据检索条件查
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            queryWrapper.and(t -> t.like("name", key).or().like("id", key));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(), queryWrapper));
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu){
        // 1.保存spu相关信息
        // 1.1保存pms_spu
        Long spuId = saveSpu(spu);

        // 1.2保存pms_spu_desc
        SpuService spuService = (SpuService) AopContext.currentProxy();
        spuService.saveSpuDesc(spu, spuId);
        // 1.3保存pms_spu_attr_value信息
        saveBaseAttr(spu, spuId);

        // 2.保存sku相关信息
        saveSku(spu);
        // FileInputStream xxx = new FileInputStream("xxx");
        //int i = 1/0;
    }
    /**
     * 保存sku相关信息及营销信息
     * @param spu
     */
    @Override
    public void saveSku(SpuVo spu) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        for (SkuVo skuVo : skus) {
            // 2.1保存pms_sku信息
            skuVo.setSpuId(spu.getId());
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            // 获取图片列表中的第一张图片作为默认图片
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ? skuVo.getDefaultImage() : images.get(0));
            }
            this.skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();
            // 2.2保存pms_sku_images
            if (!CollectionUtils.isEmpty(images)) {
                this.skuImagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(image, skuVo.getDefaultImage()) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            // 2.3保存pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                for (SkuAttrValueEntity saleAttr : saleAttrs) {
                    saleAttr.setSkuId(skuId);
                }
                this.skuAttrValueService.saveBatch(saleAttrs);
            }
            // 3.保存营销相关信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.skuSaleSave(skuSaleVo);
        }
    }
    /**
     * 保存spu基本属性信息
     * @param spu,spuId
     */
    @Override
    public void saveBaseAttr(SpuVo spu, Long spuId) {
        List<SpuAttrVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            /*for (SpuAttrVo baseAttr : baseAttrs) {
                baseAttr.setSpuId(spuId);
                spuAttrValueMapper.insert(baseAttr);
            }*/
            // 更高效的集合迭代器：流
            this.spuAttrValueService.saveBatch(baseAttrs.stream().map(spuAttrVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                // return spuAttrVo; 子类的spuId为null
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
    }
    /**
     * 保存spu描述信息（图片）
     * @param spu,spuId
     */
    @Override
    public void saveSpuDesc(SpuVo spu, Long spuId) {
        List<String> spuImages = spu.getSpuImages();
        //isBlank的话空格也视为空
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.spuDescMapper.insert(spuDescEntity);
        }
    }
    /**
     * 保存spu基本信息
     * @param spu
     */
    @Override
    public Long saveSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        Long spuId = spu.getId();
        return spuId;
    }




    public static void main(String[] args) {
        List<User> users = Arrays.asList(
                new User("柳岩", 20, false),
                new User("马蓉", 21, false),
                new User("小鹿", 22, false),
                new User("隔壁老王", 23, true),
                new User("小亮", 24, true),
                new User("百合", 25, false),
                new User("亦凡", 26, true),
                new User("柏芝", 27, false)
        );

        // map：集合之间的转化
        //users.stream().map(User::getName).collect(Collectors.toList()).forEach(System.out::println);

//        users.stream().map(user -> {
//            Person person = new Person();
//            person.setUname(user.getName());
//            person.setAge(user.getAge());
//            return person;
//        }).collect(Collectors.toList()).forEach(System.out::println);

        // filter
        //users.stream().filter(user -> user.getAge() > 22).collect(Collectors.toList()).forEach(System.out::println);
        users.stream().filter(User::getSex).collect(Collectors.toList()).forEach(System.out::println);

        // reduce
        System.out.println(users.stream().map(User::getAge).reduce((a, b) -> a + b).get());
    }
}
@Data
@AllArgsConstructor
@NoArgsConstructor
class User{
    private String name;
    private Integer age;
    private Boolean sex;
}

@Data
class Person{
    private String uname;
    private Integer age;
}