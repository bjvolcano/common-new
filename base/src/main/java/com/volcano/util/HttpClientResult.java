package com.volcano.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpClientResult {
    private Integer code;
    private String content;
    static final Integer OK = 200;

    public boolean isOK() {
        return OK.equals(code) || (code != null && code == 0);
    }

    public HttpClientResult(Integer statusCode) {
        this.code = statusCode;
    }
}
