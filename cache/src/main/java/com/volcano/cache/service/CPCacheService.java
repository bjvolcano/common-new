package com.volcano.cache.service;

import com.volcano.cache.autoconfigretion.CacheConfig;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPCacheService extends BaseCacheService {
    private static final Logger log = LoggerFactory.getLogger(CPCacheService.class);

    public CPCacheService(RedissonClient redisCommonClient, CacheConfig cacheConfig) {
        super(redisCommonClient, cacheConfig);
    }

    @Override
    public void removeKeys() {
        super.removeKeys();
    }

    @Override
    public void removeRelationCache(Collection<String> tables, Date expireAt) {
        Iterator var3 = tables.iterator();

        while (var3.hasNext()) {
            String table = (String) var3.next();
            Stream<String> keysStream = this.redissonClient.getKeys().getKeysStreamByPattern(this.applicationName + ":" + table.hashCode() + ":*");
            keysStream.forEach((keyName) -> {
                log.debug("删除直接相关缓存：{}", keyName);
                this.removeCache(keyName, expireAt);
            });
            this.removeCacheByIndirect(table, expireAt);
        }

    }

    @Override
    protected void readLock(String key) {
        try {
            log.debug("{} read lock key: {}", Thread.currentThread().getName(), key);
            this.getReadLock(key).tryLock((long) this.cacheConfig.getLockWaitTime(), this.cacheConfig.getUNIT());
        } catch (Exception e) {
            log.info("上读锁异常：{}", e.getMessage());
        }
    }

    @Override
    protected void writeLock(String key) {
        try {
            log.debug("{} write lock key: {}", Thread.currentThread().getName(), key);
            this.getWriteLock(key).tryLock((long) this.cacheConfig.getLockWaitTime(), this.cacheConfig.getUNIT());
        } catch (Exception e) {
            log.info("上写锁异常：{}", e.getMessage());
        }
    }

    @Override
    protected void unReadLock(String key) {
        log.debug("{} un read lock key: {}", Thread.currentThread().getName(), key);
        RLock readLock = this.getReadLock(key);
        if (readLock.isHeldByCurrentThread()) {
            try {
                readLock.unlock();
            } catch (Exception var4) {
                log.info("释放读锁异常：{}", var4.getMessage());
            }
        }

    }

    @Override
    protected void unWriteLock(String key) {
        log.debug("{} un write lock key: {}", Thread.currentThread().getName(), key);
        RLock writeLock = this.getWriteLock(key);
        if (writeLock.isHeldByCurrentThread()) {
            try {
                writeLock.unlock();
            } catch (Exception var4) {
                log.info("释放写锁异常：{}", var4.getMessage());
            }
        }

    }

    protected RLock getReadLock(String key) {
        return this.getReadWriteLock(key).readLock();
    }

    protected RLock getWriteLock(String key) {
        return this.getReadWriteLock(key).writeLock();
    }

    private RReadWriteLock getReadWriteLock(String key) {
        key = key + "_lock";
        RReadWriteLock readWriteLock = this.redissonClient.getReadWriteLock(key);
        return readWriteLock;
    }

    @Override
    public void writeLock(String key, Long waitTime) {
        try {
            this.getWriteLock(key).tryLock(waitTime, this.cacheConfig.getUNIT());
        } catch (InterruptedException e) {
            log.info("上等待写锁异常：{}", e.getMessage());
        }

    }
}
