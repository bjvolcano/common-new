package com.volcano.cache.service;

import com.volcano.cache.autoconfigretion.CacheConfig;
import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APCacheService extends BaseCacheService {
   private static final Logger log = LoggerFactory.getLogger(APCacheService.class);

   public APCacheService(RedissonClient redissonClient, CacheConfig cacheConfig) {
      super(redissonClient, cacheConfig);
   }

   @Override
   public void removeRelationCache(Collection<String> tables, Date expireAt) {
      tables.forEach((table) -> {
         Stream<String> keysStream = this.redissonClient.getKeys().getKeysStreamByPattern(this.applicationName + ":" + table.hashCode() + ":*");
         keysStream.forEach((keyName) -> {
            log.debug("删除直接相关缓存：{}", keyName);
            this.removeCache(keyName, expireAt);
         });
         this.removeCacheByIndirect(table, expireAt);
      });
   }
}
