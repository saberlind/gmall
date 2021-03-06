package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.UserInfo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author saberlin
 * @create 2021/12/22 19:20
 */
@Service
public class OrderService {

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {

        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // ??????????????????ThreadLocal??????userInfo
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 1.???????????????id????????????????????????
        ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressesByUserId(userId);
        List<UserAddressEntity> addressEntities = addressResponseVo.getData();
        confirmVo.setAddresses(addressEntities);

        // 2.??????????????????
        ResponseVo<List<Cart>> cartsByUserId = this.cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> carts = cartsByUserId.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new OrderException("???????????????????????????");
        }

        // ?????????????????????????????????????????????
        List<OrderItemVo> itemVos = carts.stream().map(cart ->{
            OrderItemVo orderItemVo = new OrderItemVo();

            // ????????????????????????skuId ??? count??????????????????????????????
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            // ??????skuId??????sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                orderItemVo.setTitle(skuEntity.getTitle());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setWeight(skuEntity.getWeight());
            }
            // ??????skuId??????sku???????????????
            ResponseVo<List<SkuAttrValueEntity>> responseVo = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = responseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            // ??????skuId??????????????????
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            orderItemVo.setSales(itemSaleVos);

            // ??????skuId????????????
            ResponseVo<List<WareSkuEntity>> listResponseVo = this.wmsClient.queryWareBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            return orderItemVo;
        }).collect(Collectors.toList());
        confirmVo.setItems(itemVos);

        // 3.????????????
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            confirmVo.setBounds(userEntity.getIntegration());
        }

        // ?????????????????????orderToken ?????????redis???????????????
        String orderToken = IdWorker.getIdStr();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken,orderToken,3, TimeUnit.HOURS);

        return confirmVo;
    }

    public void submit(OrderSubmitVo submitVo) {
        // 1.?????????????????????orderToken  ??????redis???orderToken
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("????????????");
        }
        // ??????????????????redis???key?????????????????????????????????????????????lua??????
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new OrderException("?????????????????????");
        }

        // 2.?????????????????????????????????  ???   ???????????????????????????  ??????
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("???????????????????????????");
        }
        // ?????????????????????????????????????????? ??????????????????
        BigDecimal currentTotalPrice = items.stream().map(item ->{
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null){
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());// ??????
        }).reduce((a,b)->a.add(b)).get();
        if (currentTotalPrice.compareTo(totalPrice) != 0){
            throw new RuntimeException("???????????????????????????????????????");
        }

        // TODO: ??????????????????

        // 3.?????????????????????(???????????????)
        List<SkuLockVo> skuLockVos = items.stream().map(item ->{
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> lockVoResponseVo = this.wmsClient.checkLock(skuLockVos,orderToken);
        List<SkuLockVo> skuLockVoList = lockVoResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)) {
            throw new OrderException("??????????????????????????????");
        }
        // ???????????????
        // int i=1/0;

        // 4.????????????
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try{
            this.omsClient.saveOrder(submitVo,userId); //feign???????????????????????????(????????????????????????????????????)
            // int i = 1/0; ??????????????????
            // ?????????????????????MQ,????????????
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.ttl",orderToken);
        }catch (Exception e){
            e.printStackTrace();
            // ?????????????????????????????????????????????,??????
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.fail",orderToken);
            throw new OrderException("?????????????????????!");
        }

        // 5.??????????????????????????????????????? MQ
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId",userId);
        msg.put("skuIds", JSON.toJSON(items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList())));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","cart.delete",msg);
    }
}
