package com.ethan.counter.service;

import com.ethan.counter.bean.res.OrderInfo;
import com.ethan.counter.bean.res.PosiInfo;
import com.ethan.counter.bean.res.TradeInfo;
import com.ethan.counter.config.CounterConfig;
import com.ethan.counter.config.GatewayConn;
import com.ethan.counter.util.DbUtil;
import com.ethan.counter.util.IDconverter;
import io.vertx.core.buffer.Buffer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thirdpart.order.CmdType;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderDirection;
import thirdpart.order.OrderType;

import java.util.List;

import static com.ethan.counter.bean.MatchDataConsumer.ORDER_DATA_CACHE_ADDR;

@Log4j2
@Component
public class OrderServiceImpl implements OrderService {
    @Override
    public Long getBalance(long uid) {
        return DbUtil.getBalance(uid);
    }

    @Override
    public List<PosiInfo> getPosiList(long uid) {
        //System.out.println(DbUtil.getPosiList(uid));
        return DbUtil.getPosiList(uid);
    }

    @Override
    public List<OrderInfo> getOrderList(long uid, String date, String code) {
        return DbUtil.getOrderList(uid,date,code);
    }

    @Override
    public List<TradeInfo> getTradeList(long uid,String date, String code) {
        return DbUtil.getTradeList(uid,date,code);
    }

    @Autowired
    private CounterConfig counterConfig;

    @Autowired
    private GatewayConn gatewayConn;

    @Override
    public boolean sendOrder(long uid, short type, long timestamp, String code, byte direction, long price, long volume, byte orderType) {

        final OrderCmd orderCmd = OrderCmd.builder()
                .type(CmdType.of(type))
                .timestamp(timestamp)
                .mid(counterConfig.getId())
                .uid(uid)
                .code(code)
                .direction(OrderDirection.of(direction))
                .price(price)
                .volume(volume)
                .orderType(OrderType.of(orderType))
                .build();

        log.info(orderCmd);
        //1. insert into db
        int oid = DbUtil.saveOrder(orderCmd);
        if(oid < 0){
            return false;
        }else{
            //send to vertex
            //1. 更新持仓数据
            if(orderCmd.direction == OrderDirection.BUY){
                DbUtil.reduceBalance(orderCmd.uid,orderCmd.price * orderCmd.volume);
                //reduce fund

            } else if (orderCmd.direction == OrderDirection.SELL) {
                DbUtil.reducePosi(orderCmd.uid,orderCmd.code,orderCmd.volume,orderCmd.price);
                //reduce repository
            }else{
                log.error("wrong direction[{}],ordercmd:{}",orderCmd.direction,orderCmd);
                return false;
            }

            //2. generate global Id [counterID, orderID]
            orderCmd.oid = IDconverter.combineIntToLong(counterConfig.getId(),oid);

            //保存委托到缓存
            //将orderCmd 序列化后 放在总线上 传递给matchdataconsumer
            byte[] serialize = null;
            try{
                serialize = counterConfig.getByteCodec().seriallize(orderCmd);
            }catch (Exception e){
                log.error("serialize ordercmd error:{}",e);
                return false;
            }
            if(serialize == null){
                return false;
            }
            counterConfig.getVertx().eventBus().send(ORDER_DATA_CACHE_ADDR, Buffer.buffer(serialize));


            //3. pack order send data ( orderCmd --> commonMsg --> tcp data stream)
            //4. send data

            gatewayConn.sendOrder(orderCmd);


            log.info(orderCmd);
            return true;
        }
    }

    @Override
    public boolean cancelOrder(int uid, int counterId, String code) {

        final OrderCmd orderCmd = OrderCmd.builder()
                .uid(uid)
                .code(code)
                .type(CmdType.CANCEL_ORDER)
                .oid(IDconverter.combineIntToLong(counterConfig.getId(), counterId))
                .build();
        log.info("recv cancel order:{}",orderCmd);

        gatewayConn.sendOrder(orderCmd);
        return false;
    }
}
