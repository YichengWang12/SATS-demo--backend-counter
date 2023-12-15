package com.ethan.counter;

import com.ethan.counter.config.CounterConfig;
import com.ethan.counter.filter.SessionCheckFilter;
import com.ethan.counter.util.DbUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import thirdpart.uuid.EthanUuid;

@SpringBootApplication
public class CounterApplication {
    @Autowired
    private DbUtil dbUtil;

    @Autowired
    private CounterConfig counterConfig;

    @PostConstruct
    private void init(){
        EthanUuid.getInstance().init(counterConfig.getDataCenterId(),counterConfig.getWorkerId());
    }

    public static void main(String[] args) {
        SpringApplication.run(CounterApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<SessionCheckFilter> sessionCheckFilterRegistration() {
        FilterRegistrationBean<SessionCheckFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SessionCheckFilter());
        registrationBean.addUrlPatterns("/*"); // 设置过滤路径
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 设置执行顺序
        return registrationBean;
    }

}
