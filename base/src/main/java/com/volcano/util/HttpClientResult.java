package com.volcano.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class HttpClientResult {
    private Integer code;
    private String content;

    public HttpClientResult() {
    }

    public HttpClientResult(Integer code, String content) {
        this.code = code;
        this.content = content;
    }

    public HttpClientResult(Integer statusCode) {
        this.code = statusCode;
    }
}
