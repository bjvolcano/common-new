package com.volcano.range.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Mapping{
    private String table;
    private String field;
    private String alias;
    public Mapping(String table,String field){
        this.table=table;
        this.field=field;
    }
}