package com.zalopay.transfer.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class Snowflake {
    private static final AtomicLong sequence = new AtomicLong(0);
    private static long machineID;
    private static final long MAX_SEQUENCE = 4095L;
    private static final long timeStampStone = 1482771600000L;

    @Value("${machine.id}")
    public void setNameStatic(long value){
        machineID = value;
    }

    public static String generateID() {
        long id = System.currentTimeMillis() - timeStampStone;
        id = (id << 10) | machineID;
        id = (id << 12) | sequence.get();
        sequence.set(sequence.get() + 1 > MAX_SEQUENCE ? 0 : sequence.get() + 1);
        return String.valueOf(id);
    }
}
