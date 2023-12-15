package com.ethan.counter.config;


import io.netty.handler.codec.CodecException;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import thirdpart.bean.CommonMsg;
import thirdpart.order.OrderCmd;
import thirdpart.tcp.TcpDirectSender;
import thirdpart.uuid.EthanUuid;

import static thirdpart.bean.MsgConstants.COUNTER_NEW_ORDER;
import static thirdpart.bean.MsgConstants.NORMAL;

@Log4j2
@Configuration
public class GatewayConn {


    @Autowired
    private CounterConfig counterConfig;

    private TcpDirectSender directSender;

    @PostConstruct
    private void init(){
        directSender = new TcpDirectSender(counterConfig.getSendIp(),counterConfig.getSendPort(),counterConfig.getVertx());
        directSender.startup();
    }

    public void sendOrder(OrderCmd orderCmd){
        byte[] data = null;
        try{
            data = counterConfig.getByteCodec().seriallize(orderCmd);
        }catch (Exception e){
            log.error("encode error for ordercmd:{}",orderCmd,e);
            return;
        }
        CommonMsg commonMsg = new CommonMsg();
        commonMsg.setBodyLength(data.length);
        commonMsg.setChecksum(counterConfig.getCs().getCheckSum(data));
        commonMsg.setMsgSrc(counterConfig.getId());
        commonMsg.setMsgDst(counterConfig.getGatewayId());
        commonMsg.setMsgType(COUNTER_NEW_ORDER);
        commonMsg.setStatus(NORMAL);
        commonMsg.setMsgNo(EthanUuid.getInstance().getUUID());
        commonMsg.setBody(data);
        directSender.send(counterConfig.getMsgCodec().encodeToBuffer(commonMsg));
    }



}
