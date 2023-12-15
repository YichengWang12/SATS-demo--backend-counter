package com.ethan.counter.filter;

import com.ethan.counter.service.AccountService;
import com.google.common.collect.Sets;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class SessionCheckFilter implements jakarta.servlet.Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Autowired
    private AccountService accountService;

    private Set<String> whiteRootPaths = Sets.newHashSet(
            "sign-in","msgsocket","test","api"
    );

    @Override
    public void doFilter(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse, jakarta.servlet.FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {

        jakarta.servlet.http.HttpServletResponse response = (jakarta.servlet.http.HttpServletResponse) servletResponse;
        response.setHeader("Access-Control-Allow-Origin","*");
        if(((jakarta.servlet.http.HttpServletRequest) servletRequest).getMethod().equals("OPTIONS")){
            response.getWriter().println("ok");
            return;
        }
        jakarta.servlet.http.HttpServletRequest request = (jakarta.servlet.http.HttpServletRequest) servletRequest;
        String path = request.getRequestURI();
        String[] split = path.split("/");
        if(split.length < 2){
            request.getRequestDispatcher(
                    "/sign-in/sign-in-fail"
            ).forward(servletRequest,servletResponse);
        }else{
            if(!whiteRootPaths.contains(split[1])){
                //check token
                if(accountService.accountExistInCache(
                        request.getParameter("token")
                )){
                    filterChain.doFilter(servletRequest,servletResponse);
                }else{
                    request.getRequestDispatcher(
                            "/sign-in/sign-in-fail"
                    ).forward(servletRequest,servletResponse);
                }
            }else{
                filterChain.doFilter(servletRequest,servletResponse);
            }
        }
    }

    @Override
    public void destroy() {

    }
}