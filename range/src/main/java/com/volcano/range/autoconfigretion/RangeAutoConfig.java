package com.volcano.range.autoconfigretion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.volcano.range.filter.MysqlFilter;
import com.volcano.range.mvc.RangeMVCInterceptor;
import com.volcano.range.mybatis.RangeInterceptor;

/**
 * @author volcano
 * @version 1.0
 * @date 2020/9/23 18:58
 */
@Order(900)
@Lazy
@Configuration
public class RangeAutoConfig implements WebMvcConfigurer{


    @Bean
    public MysqlFilter initMysqlFilter(){
        MysqlFilter mysqlFilter=new MysqlFilter();
        return mysqlFilter;
    }


    @Bean
    public RangeInterceptor initRangeInterceptor(){
        RangeInterceptor rangeInterceptor = new RangeInterceptor(initMysqlFilter());
        rangeInterceptor.setSort(-1);//最先执行
        return rangeInterceptor;
    }


    @Bean
    @Order(Integer.MAX_VALUE)
    public RangeMVCInterceptor rangeMVCInterceptor(){
        return new RangeMVCInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rangeMVCInterceptor());
    }
}
