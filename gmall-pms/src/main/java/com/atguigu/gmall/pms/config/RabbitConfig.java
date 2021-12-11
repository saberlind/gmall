package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author saberlin
 * @create 2021/12/10 20:36
 */
@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        this.rabbitTemplate.setConfirmCallback(((correlationData, ack, cause) -> {
            if (!ack) {
                // 记录日志 或者 记录到数据库，将来可以通过定时任务重新发送
                log.error("消息没有到达交换机。原因：{},元信息：{}", cause, correlationData.toString());
            }
        }));
        this.rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息没有到达队列。状态码：{},状态文本：{},交换机：{},路由键：{},消息内容：{}",replyCode,replyText,exchange,routingKey,message);
        });
    }
}
