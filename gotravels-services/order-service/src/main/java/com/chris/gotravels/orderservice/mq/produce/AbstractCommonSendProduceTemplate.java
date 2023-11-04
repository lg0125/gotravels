package com.chris.gotravels.orderservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

import java.util.Optional;

/**
 * RocketMQ 抽象公共发送消息组件
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送事件基础扩充属性实体
     *
     * @param messageSendEvent 消息发送事件
     * @return 扩充属性实体
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * 构建消息基本参数，请求头、Keys...
     *
     * @param messageSendEvent 消息发送事件
     * @param requestParam     扩充属性实体
     * @return 消息基本参数
     */
    protected abstract Message<?> buildMessage(
            T messageSendEvent,
            BaseSendExtendDTO requestParam
    );

    /**
     * 消息事件通用发送
     *
     * @param messageSendEvent 消息发送事件
     * @return 消息发送返回结果
     */
    public SendResult sendMessage(T messageSendEvent) {
        // 构建扩展参数，比如事件名称、Topic、Tag、Keys、发送超时时间、延迟消息级别
        // 因为每个消息发送生产者的参数都不一致，所以需要使用模板方法的扩展特性去收集
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);

        SendResult sendResult;
        try {
            // 构建 Topic:Tag，如果 Tag 存在则拼接，不存在只有 Topic
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag()))
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());

            // 通过 RocketMQ 模板组件发送同步消息，并设置延迟级别
            sendResult = rocketMQTemplate.syncSend(
                    destinationBuilder.toString(),
                    buildMessage(messageSendEvent, baseSendExtendDTO),
                    baseSendExtendDTO.getSentTimeout(),
                    Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0)
            );

            // 第一个参数是事件类型，方便在业务代码里进行日志搜索以及看起来一目了然
            // 并输出对应的发送结果、消息 ID、Keys 等关键信息，如果业务出现问题，这些参数能“救命”
            log.info(
                    "[{}] 消息发送结果：{}，消息ID：{}，消息Keys：{}",
                    baseSendExtendDTO.getEventName(),
                    sendResult.getSendStatus(),
                    sendResult.getMsgId(),
                    baseSendExtendDTO.getKeys()
            );
        } catch (Throwable ex) {
            // 异常后一定要打印相关的消息内容
            log.error(
                    "[{}] 消息发送失败，消息体：{}",
                    baseSendExtendDTO.getEventName(),
                    JSON.toJSONString(messageSendEvent),
                    ex
            );

            throw ex;
        }
        return sendResult;
    }
}
