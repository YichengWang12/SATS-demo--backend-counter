package com.ethan.counter.config;

import com.ethan.counter.bean.MqttBusConsumer;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thirdpart.checksum.ICheckSum;
import thirdpart.codec.IByteCodec;
import thirdpart.codec.IMsgCodec;

@Getter
@Log4j2
@Component
public class CounterConfig {
    ////////////////////////////// member id//////////////////////////
    @Value("${counter.id}")
    private short id;
    //////////////////////////////uuid config///////////////////////////
    @Value("${counter.dataCenterId}")
    private long dataCenterId;

    @Value("${counter.workerId}")
    private long workerId;

    //////////////////////////////websocket config///////////////////////
    @Value("${counter.pubport}")
    private int pubPort;

    /////////////////////////////eventbus总线配置////////////////////////
    @Value("${counter.subscribeIp}")
    private String subscribeIp;

    @Value("${counter.subscribePort}")
    private int subscribePort;


    //////////////////////////////gateway config///////////////////////

    @Value("${counter.sendip}")
    private String sendIp;

    @Value("${counter.sendport}")
    private int sendPort;

    @Value("${counter.gatewayid}")
    private short gatewayId;

    private Vertx vertx = Vertx.vertx();
    ////////////////////////////code config////////////////////////////


    @Value("${counter.checksum}")
    private String checkSumClass;

    @Value("${counter.bytecodec}")
    private  String byteCodecClass;

    @Value("${counter.msgcodec}")
    private  String msgCodecClass;

    private ICheckSum cs;
    private IByteCodec byteCodec;
    private IMsgCodec msgCodec;

    @PostConstruct
    private void init(){
        Class<?> clz;
        try{
            clz = Class.forName(checkSumClass);
            cs = (ICheckSum) clz.getDeclaredConstructor().newInstance();

            clz = Class.forName(byteCodecClass);
            byteCodec = (IByteCodec) clz.getDeclaredConstructor().newInstance();

            clz = Class.forName(msgCodecClass);
            msgCodec = (IMsgCodec) clz.getDeclaredConstructor().newInstance();

        }catch (Exception e){
            log.error("init error: ", e);
        }

        //初始化总线连接
        new MqttBusConsumer(subscribeIp,subscribePort,String.valueOf(id),msgCodec,cs,vertx).startup();
    }

}
