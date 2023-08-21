package com.volcano.classloader.config;

import com.volcano.util.HttpClientResult;
import com.volcano.util.HttpClientUtils;
import com.volcano.util.StrUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Net {
    private String url;
    private String user;
    private String password;

    public Map<String, String> buildPostArgs() {
        Map<String, String> args = new HashMap();
        args.put("user", user);
        args.put("password", password);
        return args;
    }

    public String getKeyByRemote() {
        HttpClientResult httpClientResult = HttpClientUtils.doPost(url, null, buildPostArgs());
        Integer code = httpClientResult.getCode();
        String key = StrUtils.EMPTY;
        if (code != 200) {
            throw new RuntimeException("Remote key server err : " + code);
        } else {
            key = httpClientResult.getContent();
        }

        return key;
    }
}
