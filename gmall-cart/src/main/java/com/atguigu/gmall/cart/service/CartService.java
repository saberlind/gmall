package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author saberlin
 * @create 2021/12/20 0:38
 */
@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        // 1.获取登录状态
        String userId = getUserId();
        // 2.获取该用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        // 3.判断用户的购物车是否包含该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)){
            // 包含，则更新数量
            String json = hashOps.get(skuId).toString();
            // String json = String.valueOf(hashOps.get(skuId));
            cart = JSON.parseObject(json,Cart.class);
            cart.setCount(cart.getCount().add(count));
            // 把更新后的cart对象重新写入数据库
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,skuId,cart);
        }else {
            // 不包含，则新增
            cart.setUserId(userId);
            cart.setCheck(true);
            // 根据skuId查询sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null){
                throw new CartException("您新增的商品不存在");
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());

            // 查询商品的营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> saleVos = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(saleVos));

            // 查询库存
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 查询销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            // 新增到数据库
            this.cartAsyncService.saveCart(userId,cart);
            // 新增实时价格缓存
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId,skuEntity.getPrice().toString());
        }
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    public Cart queryCart(Long skuId) {
        String userId = this.getUserId();

        // 获取到该用户的购物车 Map<skuId,cartJson>
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOps.hasKey(skuId.toString())){
            throw new CartException("您的购物车中没有该商品！");
        }
        String cartJson = hashOps.get(skuId.toString()).toString();

        return JSON.parseObject(cartJson,Cart.class);
    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = userInfo.getUserKey();
        if (userInfo.getUserId()!=null){
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    @Async
    public ListenableFuture<String> executor1(){
        try {
            System.out.println("executor1方法开始执行");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor1方法结束执行。。。");
            return AsyncResult.forValue("executor1");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);
        }
    }

    @Async
    public ListenableFuture<String> executor2() {
        try {
            System.out.println("executor2方法开始执行");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法结束执行。。。");
            return AsyncResult.forValue("executor2");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);
        }
    }

    @Async
    public String executor3() {
        try {
            System.out.println("executor2方法开始执行");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法结束执行。。。");
            int i = 1 / 0; // 制造异常
            return "executor2"; // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Cart> querycarts() {

        // 1.先查询未登录的购物车信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(unLoginKey);
        // 获取未登录的购物车集合
        List<Object> cartJsons = hashOps.values();
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(cartJsons)){
            unLoginCarts = cartJsons.stream().map(cartJson->{
                Cart cart = JSON.parseObject(cartJson.toString(),Cart.class);
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        // 2.查看是否登录，未登录直接返回
        Long userId = userInfo.getUserId();
        if (userId == null){
            return unLoginCarts;
        }
        // 3.已登录，合并购物车,并删除未登录购物车(redis+mysql)
        String loginKey = KEY_PREFIX + userId;
        // 获取登录状态购物车
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                try{
                    // 登录状态购物车已存在该商品，更新数量
                    if (loginHashOps.hasKey(cart.getSkuId().toString())){
                        // 未登录状态购物车当前商品的数量
                        BigDecimal count = cart.getCount();
                        String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                        cart = JSON.parseObject(cartJson, Cart.class);
                        cart.setCount(cart.getCount().add(count));
                        this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(),cart.getSkuId().toString(),cart);
                    }else {
                        // 登录状态不存在当前商品
                        cart.setUserId(userId.toString());
                        cart.setId(null);
                        this.cartAsyncService.saveCart(userId.toString(),cart);
                    }
                    loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
            // 清空未登录的购物车
            this.redisTemplate.delete(KEY_PREFIX + userKey);
            this.cartAsyncService.delete(userKey);
        }
        // 4.查询购物车记录(redis)
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson ->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!hashOps.hasKey(cart.getSkuId().toString())){
            throw new CartException("您要更新的购物车不存在！");
        }
        // 获取更新的数量
        BigDecimal count = cart.getCount();
        // 更新数量
        String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(cartJson, Cart.class);
        cart.setCount(count);
        hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart.getSkuId().toString(),cart);
    }

    public void deleteCart(Long skuId) {
        String userId = this.getUserId();

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(skuId.toString());

        this.cartAsyncService.deleteCartByUserIdAndSkuId(userId,skuId);
    }

    public List<Cart> queryCheckedCartsByUserId(Long userId) {

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> cartJsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartJsons)) {
            return null;
        }
        return cartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
    }
}
