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

        // 通过拦截器的ThreadLocal获取userInfo
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 1.根据用户的id查询收获地址列表
        ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressesByUserId(userId);
        List<UserAddressEntity> addressEntities = addressResponseVo.getData();
        confirmVo.setAddresses(addressEntities);

        // 2.查询送货清单
        ResponseVo<List<Cart>> cartsByUserId = this.cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> carts = cartsByUserId.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new OrderException("您没有要购买的商品");
        }

        // 把购物车集合转化成送货清单集合
        List<OrderItemVo> itemVos = carts.stream().map(cart ->{
            OrderItemVo orderItemVo = new OrderItemVo();

            // 从购物车中只获取skuId 和 count。其他字段实时去查询
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            // 根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                orderItemVo.setTitle(skuEntity.getTitle());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setWeight(skuEntity.getWeight());
            }
            // 根据skuId查询sku的销售属性
            ResponseVo<List<SkuAttrValueEntity>> responseVo = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = responseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            // 根据skuId查询营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            orderItemVo.setSales(itemSaleVos);

            // 根据skuId查询库存
            ResponseVo<List<WareSkuEntity>> listResponseVo = this.wmsClient.queryWareBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            return orderItemVo;
        }).collect(Collectors.toList());
        confirmVo.setItems(itemVos);

        // 3.用户积分
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            confirmVo.setBounds(userEntity.getIntegration());
        }

        // 防重：生成一个orderToken 页面和redis都需要一份
        String orderToken = IdWorker.getIdStr();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken,orderToken,3, TimeUnit.HOURS);

        return confirmVo;
    }

    public void submit(OrderSubmitVo submitVo) {
        // 1.防重：页面上的orderToken  查询redis中orderToken
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("非法请求");
        }
        // 验证通过要对redis中key进行删除，为了保证原子性，使用lua脚本
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new OrderException("请不要重复提交");
        }

        // 2.验总价：页面上的总价格  和   数据库中实时总价格  比较
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("您没有要购买的商品");
        }
        // 遍历订单详情，获取数据库价格 计算实时总价
        BigDecimal currentTotalPrice = items.stream().map(item ->{
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null){
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());// 小计
        }).reduce((a,b)->a.add(b)).get();
        if (currentTotalPrice.compareTo(totalPrice) != 0){
            throw new RuntimeException("页面已过期，请刷新后重试！");
        }

        // TODO: 限购件数验证

        // 3.验库存并锁库存(保证原子性)
        List<SkuLockVo> skuLockVos = items.stream().map(item ->{
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> lockVoResponseVo = this.wmsClient.checkLock(skuLockVos,orderToken);
        List<SkuLockVo> skuLockVoList = lockVoResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)) {
            throw new OrderException("对不起库存不足！！！");
        }
        // 服务器宕机
        // int i=1/0;

        // 4.创建订单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try{
            this.omsClient.saveOrder(submitVo,userId); //feign（请求，响应）超时(需要配置延时队列定时关单)
            // int i = 1/0; 模拟下单失败
            // 发送延时消息到MQ,定时关单
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.ttl",orderToken);
        }catch (Exception e){
            e.printStackTrace();
            // 如果创建订单失败，立马释放库存,关单
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","order.fail",orderToken);
            throw new OrderException("服务器内部错误!");
        }

        // 5.异步删除购物车中对应的记录 MQ
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId",userId);
        msg.put("skuIds", JSON.toJSON(items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList())));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","cart.delete",msg);
    }
}
