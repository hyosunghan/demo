package com.example.demo._identity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;

/**
 * 雪花算法
 *
 * 64位ID: 1标记位 41位时间位 11位机器码 11位序列号
 * 1 11111111111111111111111111111111111111111 11111111111 11111111111
 */
@Slf4j
@Component
public class SnowFlakeIdentity {

    /**
     * 时间戳基准值2025-01-01T00:00:00.000Z，时间戳截止值2094-09-07T15:47:35.551Z
     */
    private static final long TIMESTAMP_DATUM = Instant.parse("2025-01-01T00:00:00.000Z").toEpochMilli();

    /**
     * 序列号位数
     */
    private static final int SEQUENCE_BITS = 11;

    /**
     * 机器码位数
     */
    private static final int MACHINE_NUMBER_BITS = 11;

    /**
     * 最大序列号:2047(该算法可计算固定位数的二进制所能表达的最大十进制数)
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 历史时间戳，默认从当前开始，后续自增
     */
    private long pastTimeStamp = System.currentTimeMillis();

    /**
     * 当前序列号，默认从0开始，后续自增
     */
    private long currentSequence = 0;

    @Value("${system.node-id:1}")
    private int nodeId;

    @Value("${snowflake.clock.rollback.limit:5000}")
    private int clockRollbackLimit;

    @PostConstruct
    public void init() {
        if (nodeId < 1 || nodeId > MAX_SEQUENCE) {
            throw new IllegalStateException(
                    String.format("Node id must be between 1 and %d, got: %d", MAX_SEQUENCE, nodeId));        }
    }

    private SnowFlakeIdentity() {

    }

    /**
     * 下一个ID
     *
     * @return  nextId
     */
    public synchronized long nextId() {
        // 序列号重置及历史时间戳维护
        if (currentSequence > MAX_SEQUENCE) {
            pastTimeStamp++;
            currentSequence = 0;
        }
        // 历史事件戳有效性检查
        long currentTimeStamp;
        do {
            currentTimeStamp = System.currentTimeMillis();
            // 1. 历史时间戳小于当前时间戳，服务正常
            if (pastTimeStamp < currentTimeStamp) {
                break;
            }
            // 2. 发生时钟回拨，且回拨时间超过积攒的历史时间戳超过配置时间
            if (pastTimeStamp - currentTimeStamp > clockRollbackLimit) {
                log.error("snowflake clock rollback detected limit {}, service is unavailable.", clockRollbackLimit);
                throw new IllegalStateException("Large-scale clock rollback detected, service is unavailable.");
            }
            // 3. 发生时钟回拨且回拨时间小于配置时间，或者ID生成速率从该类初始化开始一直超满载生成，服务等待继续
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } while (true);
        // 生成ID
        return (pastTimeStamp - TIMESTAMP_DATUM) << MACHINE_NUMBER_BITS << SEQUENCE_BITS
                | currentSequence++ << MACHINE_NUMBER_BITS
                | nodeId;
    }
}
