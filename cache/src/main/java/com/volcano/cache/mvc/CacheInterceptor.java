package com.volcano.cache.mvc;

import com.volcano.cache.service.ICacheService;
import com.volcano.mvc.BaseMVCInterceptor;
import lombok.extern.slf4j.Slf4j;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 数据范围mvc拦截器
 */
@Slf4j
public class CacheInterceptor extends BaseMVCInterceptor {

    ICacheService cacheService;
    public void setCacheService(ICacheService cacheService){
        this.cacheService=cacheService;
    }
    /**
     * 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.debug("清理缓存资源");
        cacheService.removeKeys();
    }

}