package com.volcano.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Objects;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/10/28 13:01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlEntity {
    private Collection<String> tables;
    private Integer tableHashCode;
    private Integer sqlHashCode;
    private Integer sqlArgValuesHashCode;
    private String sql;
    private boolean isRead;
    private String key;
    private Object params;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlEntity entity = (SqlEntity) o;
        return Objects.equals(key, entity.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
