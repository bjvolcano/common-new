package com.volcano.test.controller;

import com.volcano.test.encrypt.component.Components;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Order
@RestController
public class TestController {
    @Autowired
    private Components components;

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/test")
    public String test(String args) {
        return components.test(args);
    }
}
