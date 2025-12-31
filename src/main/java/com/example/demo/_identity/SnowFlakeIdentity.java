package com.example.demo._identity;

import java.time.Instant;

/**
 * 雪花算法
 *
 * 64位ID: 1标记位 41位时间位 11位机器码 11位序列号
 * 1 11111111111111111111111111111111111111111 11111111111 11111111111
 */
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

    private static SnowFlakeIdentity instance;

    /**
     * 机器码
     */
    public static int machineNumber = -1;

    /**
     * 历史时间戳，默认从当前开始，后续自增
     */
    private static long pastTimeStamp = System.currentTimeMillis();

    /**
     * 当前序列号，默认从0开始，后续自增
     */
    private static long currentSequence = 0;


    private SnowFlakeIdentity() {

    }

    public static SnowFlakeIdentity getInstance() {
        if (instance == null) {
            synchronized(SnowFlakeIdentity.class) { // 注意这里是类级别的锁
                if (instance == null) {       // 这里的检测避免多线程并发时多次创建对象
                    if (machineNumber < 0) {
                        // 机器码未初始化，不能生成正确的机器码
                        throw new IllegalStateException("Please wait machine number allot.");
                    }
                    instance = new SnowFlakeIdentity();
                }
            }
        }
        return instance;
    }

    /**
     * 下一个ID
     *
     * @return  nextId
     */
    public synchronized long nextId() {
        if (currentSequence > MAX_SEQUENCE) {
            pastTimeStamp++;
            currentSequence = 0;
        }
        while (pastTimeStamp >= System.currentTimeMillis()) {
            // wait time go
        }
        return (pastTimeStamp - TIMESTAMP_DATUM) << MACHINE_NUMBER_BITS << SEQUENCE_BITS
                | currentSequence++ << MACHINE_NUMBER_BITS
                | machineNumber;
    }
}
