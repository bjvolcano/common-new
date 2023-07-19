package com.volcano.range.dto;


public abstract class AbstractRangeData implements IRangeData {
    protected final ThreadLocal data = new ThreadLocal();
    public <T> T get(){
        return (T)data.get();
    }
    public void set(Object object){
        data.set(object);
    }
    @Override
    public void remove(){
        data.remove();
    }
}
