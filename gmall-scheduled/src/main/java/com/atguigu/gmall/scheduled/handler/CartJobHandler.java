package com.atguigu.gmall.scheduled.handler;

import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/21 15:27
 */
@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String EXCEPTION_KEY = "cart:async:exception";
    private static final String KEY_PREFIX = "cart:info:";

    @XxlJob("cartSyncData")
    public ReturnT<String> syncData(String param){

        // 获取异步任务失败信息 userId集合
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();

        while (StringUtils.isNotBlank(userId)){

            // 直接清空mysql中的数据
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId));

            // 获取当前失败用户的redis中的购物车 Map<skuid,cartJson>
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            if (CollectionUtils.isEmpty(cartJsons)){
                return ReturnT.SUCCESS;
            }

            cartJsons.forEach(cartJson ->{
                try {
                    Cart cart = MAPPER.readValue(cartJson.toString(), Cart.class);
                    cart.setId(null);
                    this.cartMapper.insert(cart);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });

            // 获取下一个
            userId = setOps.pop();
        }
        return ReturnT.SUCCESS;
    }
}
