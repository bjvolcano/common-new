package com.volcano.test.encrypt.component;

import com.volcano.interfaces.ITest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @Author bjvolcano
 * @Date 2021/5/11 9:41 上午
 * @Version 1.0
 */
@Component
public class Components {
    @Autowired
    private ITest iTest;

    public String test(String args){
       return iTest.test(args);
    }
}
