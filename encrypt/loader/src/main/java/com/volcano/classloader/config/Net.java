package com.volcano.classloader.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Net {
    private String url;
    private String user;
    private String password;

    public Map<String, String> buildPostArgs(){
        Map<String, String> args= new HashMap();
        args.put("user",user);
        args.put("password",password);
        return args;
    }
}
