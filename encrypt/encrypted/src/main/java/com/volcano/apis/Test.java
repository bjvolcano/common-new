package com.volcano.apis;

import com.volcano.interfaces.ITest;
import org.springframework.stereotype.Component;

/**
 * @Author bjvolcano
 * @Date 2021/5/7 6:56 下午
 * @Version 1.0
 */

@Component
public class Test implements ITest {
    @Override
    public String test(String args) {
        return "input args : {"+args+"}";
    }
}
