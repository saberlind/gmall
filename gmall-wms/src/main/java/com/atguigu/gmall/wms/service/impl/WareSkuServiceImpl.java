package com.atguigu.gmall.wms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String LOCK_PREFIX = "stock:lock:";
    private static final String KEY_PREFIX = "stock:info:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<WareSkuEntity> queryWareBySkuId(Long skuId) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_id",skuId);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional
    public List<SkuLockVo> checkLock(List<SkuLockVo> lockVos, String orderToken) {
        if (CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("您没有要购买的商品");
        }

        // 遍历所有商品  验库存并锁库存
        lockVos.forEach(lockVo ->{
            this.checkAndLock(lockVo);
        });

        // 判断是否存在锁定失败的商品，如果存在则把锁定成功的仓库解锁
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())){
            lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList()).forEach(lockVo ->{
                this.wareSkuMapper.unlock(lockVo.getWareId(),lockVo.getCount());
            });
            // 返回锁定消息
            return lockVos;
        }

        // 为了方便将来解锁库存  或者  减库存
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken,JSON.toJSONString(lockVos));

        // 防止锁定库存后服务器宕机，导致库存一直被锁。
        // TODO 定时解锁库存，发送延时消息
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.ttl",orderToken);
        // 都锁定成功，返回null
        return null;
    }

    private void checkAndLock(SkuLockVo lockVo) {
        RLock fairLock = this.redissonClient.getFairLock(LOCK_PREFIX + lockVo.getSkuId());
        fairLock.lock();

        try {
            // 1.验库存
            List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.check(lockVo.getSkuId(),lockVo.getCount());
            if (CollectionUtils.isEmpty(wareSkuEntities)){
                lockVo.setLock(false);
                return;
            }
            // 这里获得的是有货的仓库的集合，可以根据业务需求进行判断具体选择哪一个仓库进行发货
            // 2.锁库存
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            if (this.wareSkuMapper.lock(wareSkuEntity.getId(),lockVo.getCount()) ==1){
                lockVo.setLock(true);
                lockVo.setWareId(wareSkuEntity.getId());
            }else {
                lockVo.setLock(false);
            }
        }finally {
            fairLock.unlock();
        }
    }
}