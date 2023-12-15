package com.ethan.counter.service;

import com.ethan.counter.bean.res.OrderInfo;
import com.ethan.counter.bean.res.PosiInfo;
import com.ethan.counter.bean.res.TradeInfo;

import java.util.List;

public interface OrderService {
    //query balance
    Long getBalance(long uid);

    //query position
    List<PosiInfo> getPosiList(long uid);
    //query order
    List<OrderInfo> getOrderList(long uid,String date, String code);

    //query trade
    List<TradeInfo> getTradeList(long uid,String date, String code);

    boolean sendOrder(long uid,short type,long timestamp,String code, byte direction,long price,long volume,byte orderType);

    boolean cancelOrder(int uid, int counterId, String code);
}
