package com.volcano.cache.autoconfigretion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@ConfigurationProperties(prefix = "common.cache")
@Data
public class CacheConfig {
    private Integer lockWaitTime;
    private String mode;
    private TimeUnit UNIT = TimeUnit.MILLISECONDS;
    private Integer hotKeyTime;
    private Integer hotKeyCount;
}
