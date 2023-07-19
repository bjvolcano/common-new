package com.volcano.test.config.range;

import com.volcano.conf.BaseConfig;
import com.volcano.range.filter.AbstructRangeFilter;
import com.volcano.range.filter.IRangeFilter;
import com.volcano.range.filter.MysqlFilter;
import com.volcano.range.mapping.ITableMapping;
import com.volcano.range.mapping.Mapping;
import com.volcano.range.mvc.RangeMVCInterceptor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class RangConfig extends BaseConfig implements WebMvcConfigurer {

    @Autowired
    RangeMVCInterceptor rangeMVCInterceptor;
    @Bean
    public ITableMapping initUserRangeTableMapping() {
        return new UserTableMapping();
    }

    //此处初始化不是必须，根据业务需要，在语句中包含了要数据隔离得 表 ，并且包含配置得排除表，则不会过滤
    @Bean
    public ITableMapping initUserRangeExcludeTableMapping() {
        UserTableMapping exclude = new UserTableMapping();
        List<Mapping> mappings = new ArrayList<>();
        //mappings.add(new Mapping("排除得表",""));
        exclude.setMappings(mappings);
        return exclude;
    }

    /**
     * 设置数据隔离拦截器得排除地址
     * @param registry
     */
    @SneakyThrows
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        Field registrations = registry.getClass().getDeclaredField("registrations");
        registrations.setAccessible(true);
        List<InterceptorRegistration> interceptors = (List<InterceptorRegistration>) registrations.get(registry);

        if(!CollectionUtils.isEmpty(interceptors)) {
            for(InterceptorRegistration in : interceptors){
                HandlerInterceptor interceptor = getInterceptor(in);
                if(interceptor instanceof RangeMVCInterceptor) {
                    in.excludePathPatterns("/sys/login", "/sys/code/*");
                    log.info("设置数据隔离拦截器 排除地址--->ok");
                }
            }
        }
    }



    @Bean
    public UserRangeRangeData userRangeRangeData(){
        return new UserRangeRangeData();
    }

    /**
     * 本方法配置过滤规则 与 排除规则
     * @return
     */
    @Bean
    public IRangeFilter initUserRangeFilter() {
        IRangeFilter filter = new UserRangeFilter(userRangeRangeData());
        ((AbstructRangeFilter) filter).setMapping(initUserRangeTableMapping());

        //当遇到该表时，本次不做过滤 和 映射配置 配合使用，此处不时必须，根据业务需要
        //此处初始化不是必须，根据业务需要，在语句中包含了要数据隔离得 表 ，并且包含配置得排除表，则不会过滤
        ((AbstructRangeFilter) filter).setExclude(initUserRangeExcludeTableMapping());
        return filter;
    }

    /**
     * 本方法配置过滤规则 与 排除规则
     * @return
     */
    @Bean
    public IRangeFilter initAdminRangeFilter() {
        IRangeFilter filter = new AdminRangeFilter(userRangeRangeData());
        UserTableMapping tableMapping=new UserTableMapping();
        List<Mapping> mappings = new ArrayList<>();
        mappings.add(new Mapping("sys_user","user_id"));
        tableMapping.setMappings(mappings);
        ((AbstructRangeFilter) filter).setMapping(tableMapping);
        return filter;
    }

    /**
     * 向mvc拦截器中增加范围数据收集得对象 mvc会自动收集
     * @return
     */
    @Bean
    @SuppressWarnings("unchecked")
    public Object fillMvcRangDatas(){

        rangeMVCInterceptor.addRangeData(userRangeRangeData());

        return "fillMvcRangDatas";
    }



    @Bean
    @SuppressWarnings("unchecked")
    public Object fillRangFilters(MysqlFilter mysqlFilter) {
        //在此添加数据隔离过滤规则
        IRangeFilter iRangeFilter = initUserRangeFilter();
        List<Mapping> mappings = new ArrayList<>();
        //mappings.add(new Mapping("排除得表",""));
        ITableMapping exclude=new UserTableMapping();
        exclude.setMappings(mappings);
        ((AbstructRangeFilter) iRangeFilter).setExclude(exclude);
        mysqlFilter.addFilter(iRangeFilter);

        mysqlFilter.addFilter(initAdminRangeFilter());
        //多个规则得话，添加多次
        return "mysqlFilterFill";
    }

}
