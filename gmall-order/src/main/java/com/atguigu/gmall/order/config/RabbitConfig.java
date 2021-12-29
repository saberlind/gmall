package com.atguigu.gmall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author saberlin
 * @create 2021/12/23 11:40
 */
@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack){
                // 记录日志 或者 记录到数据库，将来可以通过定时任务重新发送
                log.error("消息么有到达交换机。原因：{}, 元信息：{}", cause, correlationData.toString());
            }
        });
        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息没有到达队列。状态码：{}，状态文本：{}，交换机：{}，路由键：{}，消息内容：{}", replyCode, replyText, exchange, routingKey, new String(message.getBody()));
        });
    }

    /**
     * 延时交换机：ORDER_EXCHANGE
     */

    /**
     * 延时队列：ORDER_TTL_QUEUE
     */
    @Bean
    public Queue queue(){
        return QueueBuilder.durable("ORDER_TTL_QUEUE").ttl(90000)
                .deadLetterRoutingKey("ORDER_EXCHANGE")
                .deadLetterRoutingKey("ORDER.DEAD").build();
    }

    /**
     * 延时队列绑定延时交换机:order.ttl
     */
    @Bean
    public Binding binding(){
        return new Binding("ORDER_TTL_QUEUE", Binding.DestinationType.QUEUE,
                "ORDER_EXCHANGE", "order.ttl", null);
    }


    /**
     * 死信交换机：ORDER_EXCHANGE
     */

    /**
     * 死信队列：ORDER_DEAD_QUEUE
     */
    @Bean
    public Queue deadQueue(){
        return QueueBuilder.durable("ORDER_DEAD_QUEUE").build();
    }

    /**
     * 把死信队列绑定到死信交换机：order.dead
     */
    @Bean
    public Binding deadBinding(){
        return new Binding("ORDER_DEAD_QUEUE", Binding.DestinationType.QUEUE,
                "ORDER_EXCHANGE", "order.dead", null);
    }
}
