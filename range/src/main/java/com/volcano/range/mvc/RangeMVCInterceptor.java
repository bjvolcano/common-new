package com.volcano.range.mvc;

import com.volcano.mvc.BaseMVCInterceptor;
import com.volcano.range.dto.AbstractRangeData;
import com.volcano.range.dto.IRangeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据范围mvc拦截器
 */
@Order(Integer.MAX_VALUE)
@Slf4j
public class RangeMVCInterceptor extends BaseMVCInterceptor {


    private List<IRangeData> rangeDates =new ArrayList<>();


    public void addRangeData(AbstractRangeData rangeData){
        rangeDates.add(rangeData);
    }
    /**
     * 在请求处理之前进行调用（Controller方法调用之前）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)  {

        if(request.getMethod().equals("OPTIONS")) {
            return true;
        }
        //在此收集用户的范围数据，通过
        log.debug("收集用户的范围数据");
        //RangeData.set(...);
        rangeDates.forEach(x->x.fillRangeData());
        return true;
    }

    /**
     * 请求处理之后进行调用，但是在视图被渲染之前（Controller方法调用之后）
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    /**
     * 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.debug("清理 rangefilter 数据");
        rangeDates.forEach(x->x.remove());
    }

}