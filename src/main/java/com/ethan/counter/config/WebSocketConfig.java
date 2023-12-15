package com.ethan.counter.config;


import io.vertx.core.Vertx;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class WebSocketConfig {

    public static final String L1_MARKET_DATA_PREFIX = "l1-market-data";

    public static final String TRADE_NOTIFY_ADDR_PREFIX = "tradechange-";

    public static final String ORDER_NOTIFY_ADDR_PREFIX = "orderchange-";

    @Autowired
    private CounterConfig counterConfig;




    @PostConstruct
    private void init() {
        Vertx vertx = counterConfig.getVertx();

        //只允许成交 委托的变动通过websocket总线往外发送， 只允许行情数据从websocket总线进入
        SockJSBridgeOptions options = new SockJSBridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress(L1_MARKET_DATA_PREFIX))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(ORDER_NOTIFY_ADDR_PREFIX))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(TRADE_NOTIFY_ADDR_PREFIX));
        log.info("websocket config:{}", counterConfig);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        sockJSHandler.bridge(options,event ->{
            if(event.type() == BridgeEventType.SOCKET_CREATED){
                log.info("A socket was created : {}", event.socket().remoteAddress());
            }else if(event.type() == BridgeEventType.SOCKET_CLOSED){
                log.info("A socket was closed : {}", event.socket().remoteAddress());
            }
            event.complete(true);
        });

        //制定websocket的url
        Router router = Router.router(vertx);
        router.route("/eventbus/*").handler(sockJSHandler);
        vertx.createHttpServer().requestHandler(router).listen(counterConfig.getPubPort());

    }
}
