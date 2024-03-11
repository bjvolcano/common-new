package com.volcano.sync.mvc;

import com.volcano.mvc.BaseMVCInterceptor;
import com.volcano.sync.mybatis.UpdateCacheInterceptor;
import com.volcano.sync.service.ISyncService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 同步数据mvc拦截器
 */
@Slf4j
public class SyncInterceptor extends BaseMVCInterceptor {

    @Resource
    private ISyncService syncService;

    /**
     * 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        syncService.remove();
        log.debug("清理缓存资源");
    }

}