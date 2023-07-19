package com.volcano.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/11/4 11:15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlParams {
    private String sql;

    private String key;

    public SqlParams(String sql){
        this.sql=sql;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlParams sqlParams = (SqlParams) o;
        return Objects.equals(sql, sqlParams.sql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql);
    }
}
