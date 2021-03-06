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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
@Slf4j
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
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(paramVo.getPage(),new QueryWrapper<SpuEntity>());
        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuInfo(PageParamVo pageParamVo, Long cid) {
        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();
        // ????????????id??????0??????????????????id??????
        if (cid != 0) {
            queryWrapper.eq("category_id", cid);
        }
        // ????????????????????????????????????????????????????????????
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            queryWrapper.and(t -> t.like("name", key).or().like("id", key));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(), queryWrapper));
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu){
        // 1.??????spu????????????
        // 1.1??????pms_spu
        Long spuId = saveSpu(spu);

        // 1.2??????pms_spu_desc
        SpuService spuService = (SpuService) AopContext.currentProxy();
        spuService.saveSpuDesc(spu, spuId);
        // 1.3??????pms_spu_attr_value??????
        saveBaseAttr(spu, spuId);

        // 2.??????sku????????????
        saveSku(spu);
        // FileInputStream xxx = new FileInputStream("xxx");
        //int i = 1/0;
        sendMessage(spu.getId(),"insert");
    }
    /**
     * ??????sku???????????????????????????
     * @param spu
     */
    @Override
    public void saveSku(SpuVo spu) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        for (SkuVo skuVo : skus) {
            // 2.1??????pms_sku??????
            skuVo.setSpuId(spu.getId());
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            // ?????????????????????????????????????????????????????????
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ? skuVo.getDefaultImage() : images.get(0));
            }
            this.skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();
            // 2.2??????pms_sku_images
            if (!CollectionUtils.isEmpty(images)) {
                this.skuImagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(image, skuVo.getDefaultImage()) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            // 2.3??????pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                for (SkuAttrValueEntity saleAttr : saleAttrs) {
                    saleAttr.setSkuId(skuId);
                }
                this.skuAttrValueService.saveBatch(saleAttrs);
            }
            // 3.????????????????????????
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.skuSaleSave(skuSaleVo);
        }
    }
    /**
     * ??????spu??????????????????
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
            // ?????????????????????????????????
            this.spuAttrValueService.saveBatch(baseAttrs.stream().map(spuAttrVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                // return spuAttrVo; ?????????spuId???null
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
    }
    /**
     * ??????spu????????????????????????
     * @param spu,spuId
     */
    @Override
    public void saveSpuDesc(SpuVo spu, Long spuId) {
        List<String> spuImages = spu.getSpuImages();
        //isBlank????????????????????????
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.spuDescMapper.insert(spuDescEntity);
        }
    }
    /**
     * ??????spu????????????
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

    public void sendMessage(Long spuId,String type){
        try {
            // ????????????
            this.rabbitTemplate.convertAndSend("PMS_SPU_EXCHANGE","item." + type , spuId);
        } catch (AmqpException e) {
            log.error("{}?????????????????????????????????id:{}",type,spuId);
        }
    }

    public static void main(String[] args) {
        List<User> users = Arrays.asList(
                new User("??????", 20, false),
                new User("??????", 21, false),
                new User("??????", 22, false),
                new User("????????????", 23, true),
                new User("??????", 24, true),
                new User("??????", 25, false),
                new User("??????", 26, true),
                new User("??????", 27, false)
        );

        // map????????????????????????
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