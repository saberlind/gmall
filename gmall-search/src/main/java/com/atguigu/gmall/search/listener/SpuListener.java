package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.search.service.SearchService;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author saberlin
 * @create 2021/12/10 20:57
 */
@Component
public class SpuListener {

    @Autowired
    private SearchService searchService;


    /**
     * 监听insert方法
     * @param spuId
     * @param channel
     * @param message
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_INSERT_QUEUE",durable = "true"),
            exchange = @Exchange(
                    value = "PMS_SPU_EXCHANGE",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC),
            key = {"item.insert"}))
    public void listenCreate(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        // 创建索引
        this.searchService.createIndex(spuId,channel,message);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
