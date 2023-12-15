package com.ethan.counter.controller;

import com.ethan.counter.bean.res.*;
import com.ethan.counter.cache.CacheType;
import com.ethan.counter.cache.RedisStringCache;
import com.ethan.counter.cache.StockCache;
import com.ethan.counter.service.AccountService;
import com.ethan.counter.service.OrderService;
import com.ethan.counter.util.Captcha;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thirdpart.uuid.EthanUuid;

import java.util.Collection;
import java.util.List;

import static com.ethan.counter.bean.res.CounterRes.*;

@RestController
@RequestMapping("/api")
@Log4j2
public class OrderController {

    @Autowired
    private StockCache stockCache;

    @RequestMapping("/code")
    private CounterRes stockQuery(@RequestParam String key){
        Collection<StockInfo> stockInfos = stockCache.getStock(key);
        return new CounterRes(stockInfos);
    };

    @Autowired
    private OrderService orderService;


    @RequestMapping("/balance")
    public CounterRes balanceQuery(@RequestParam long uid) throws Exception{
        long balance = orderService.getBalance(uid);
        return new CounterRes(balance);
    };

    @RequestMapping("/posiInfo")
    public CounterRes posiQuery(@RequestParam long uid) throws Exception{
        List<PosiInfo> posiInfos = orderService.getPosiList(uid);
//        System.out.println(posiInfos);
        return new CounterRes(posiInfos);
    }

    @RequestMapping("/orderInfo")
    public CounterRes orderQuery(@RequestParam long uid,String date,String code) throws Exception{
        if(code == null){
            code = "";
        }else{
            code = code + "%";
        }
        if(date == null){
            date = "";
        }
        List<OrderInfo> orderInfos = orderService.getOrderList(uid,date,code);
        return new CounterRes(orderInfos);
    }

    @RequestMapping("/tradeInfo")
    public CounterRes tradeQuery(@RequestParam long uid,String date, String code) throws Exception{
        if(code == null){
            code = "";
        }else{
            code = code + "%";
        }
        if(date == null){
            date = "";
        }
        List<TradeInfo> tradeInfos = orderService.getTradeList(uid,date,code);
        return new CounterRes(tradeInfos);
    }

    @RequestMapping("/sendorder")
    public CounterRes order(
            @RequestParam int uid,
            @RequestParam short type,
            @RequestParam long timestamp,
            @RequestParam String code,
            @RequestParam byte direction,
            @RequestParam long price,
            @RequestParam long volume,
            @RequestParam byte orderType
    ){
        if(orderService.sendOrder(uid,type,timestamp,code,direction,price,volume,orderType)){
            return new CounterRes(SUCCESS,"sent order successfully",null);
        }else{
            return new CounterRes(FAIL,"failed to send order",null);
        }
    }

    @RequestMapping("/cancelorder")
    public CounterRes cancelOrder(
            @RequestParam int uid,
            @RequestParam int counterId,
            @RequestParam String code
    ){
        if(orderService.cancelOrder(uid,counterId,code)){
            return new CounterRes(SUCCESS,"cancel order successfully",null);
        }else{
            return new CounterRes(FAIL,"failed to cancel order",null);
        }
    }
}
