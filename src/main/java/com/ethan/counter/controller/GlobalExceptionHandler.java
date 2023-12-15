package com.ethan.counter.controller;


import com.ethan.counter.bean.res.CounterRes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
@Log4j2
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public CounterRes exceptionHandler(HttpServletRequest request, Exception e){
        log.error(e);
        return new CounterRes(CounterRes.FAIL,"error!",null);
    }
}
