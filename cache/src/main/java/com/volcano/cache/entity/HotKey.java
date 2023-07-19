package com.volcano.cache.entity;

import lombok.Data;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class HotKey {
    private AtomicInteger count = new AtomicInteger(0);
    private Long lastTime;

    public void zero(){
        count.set(0);
    }
}
