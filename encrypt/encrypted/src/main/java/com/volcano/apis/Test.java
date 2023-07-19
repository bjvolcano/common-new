package com.volcano.apis;

import com.volcano.interfaces.ITest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @Author bjvolcano
 * @Date 2021/5/7 6:56 下午
 * @Version 1.0
 */
@Lazy
@Slf4j
@Component
@Order
public class Test implements ITest {
    @Override
    public String test(String args) {
        log.info("----------do Test implement----------");
        return "input args : {"+args+"}";
    }

    @PostConstruct
    public void init() {
        log.info(this.getClass().getClassLoader().getClass().getName() + " test inited");
    }
}
