package com.chris.gotravels.ticketservice.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.chris.gotravels.frameworks.pattern.strategy.AbstractStrategyChoose;
import com.chris.gotravels.ticketservice.common.constant.TicketRocketMQConstant;
import com.chris.gotravels.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.chris.gotravels.ticketservice.mq.event.CanalBinlogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 列车车票余量缓存更新消费端
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.CANAL_COMMON_SYNC_TOPIC_KEY,
        consumerGroup = TicketRocketMQConstant.CANAL_COMMON_SYNC_CG_KEY
)
public class CanalCommonSyncBinlogConsumer implements RocketMQListener<CanalBinlogEvent> {

    private final AbstractStrategyChoose abstractStrategyChoose;

    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    @Override
    public void onMessage(CanalBinlogEvent message) {
        // 如果是 DDL 返回
        // 如果不是 UPDATE 类型数据变更返回
        // 如果没有开启 binlog 数据同步模型返回
        if (message.getIsDdl()
                || CollUtil.isEmpty(message.getOld())
                || !Objects.equals("UPDATE", message.getType())
                || !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog"))
            return;

        // 通过策略模式进行不同 Binlog 变更类型的监听，
        // 比如，订单和座位两个表就分别有两个处理类
        abstractStrategyChoose.chooseAndExecute(
                message.getTable(),
                message,
                CanalExecuteStrategyMarkEnum.isPatternMatch(message.getTable())
        );
    }

}
