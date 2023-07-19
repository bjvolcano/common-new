package com.volcano.cache.service;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.executor.Executor;

public interface ICacheService {
   String DB_TYPE = "mysql";
   Long EXPIRE = 300L;

   Object get(String key);

   Object get();

   Object getBySqlKey(String sql, Object argValues, Executor executor) throws SQLException;

   boolean put(String key, Object value);

   boolean put(String key, Object value, Long expire, TimeUnit unit);

   boolean put(Object value, Long expire, TimeUnit unit);

   String getKey(String sql, Object argValues);

   String getKey();

   void setKey(String sql);

   void remove();

   void removeKeys();

   void writeLock(String key, Long waitTime);

   void hotKeyLock(String key);

   void unHotKeyLock(String key);
}
