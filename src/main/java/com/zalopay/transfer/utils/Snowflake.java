package com.zalopay.transfer.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class Snowflake {
    private static long sequence = 0L;
    private static long machineID;
    private static final long MAX_SEQUENCE = (1L << 12) - 1;;
    private static final long timeStampStone = 1482771600000L;
    private static volatile long lastTimestamp = -1L;

    @Value("${machine.id}")
    public void setNameStatic(long value){
        machineID = value;
    }

    public synchronized static String generateID() {
        long currentTimestamp = timestamp();

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if(sequence == 0) {
                while (currentTimestamp == lastTimestamp) {
                    currentTimestamp = timestamp();
                }
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = currentTimestamp;
        long id = (currentTimestamp << 22) | (machineID << 12) | sequence;
        return String.valueOf(id);
    }

    private static long timestamp() {
        return Instant.now().toEpochMilli() - timeStampStone;
    }
}
