package com.volcano.range.mapping;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bjvolcano
 */
public abstract class ITableMapping implements InitializingBean {
    protected List<Mapping> mappings;


    public List<Mapping> getMappings() {
        return mappings;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (CollectionUtils.isEmpty(mappings)) {
            initMapping();
        }
    }

    /**
     * 初始化方法，每个模块初始化自己的表名、字段名映射
     */
    public abstract void initMapping();

    public synchronized void addMapping(Mapping mapping) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        mappings.add(mapping);
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

}