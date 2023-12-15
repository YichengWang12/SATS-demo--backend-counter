package com.ethan.counter.bean;

import com.ethan.counter.config.CounterConfig;
import com.ethan.counter.util.JsonUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thirdpart.hq.L1MarketData;

import java.util.HashMap;
import java.util.Map;

import static com.ethan.counter.bean.MqttBusConsumer.INNER_MARKET_DATA_CACHE_ADDR;
import static com.ethan.counter.config.WebSocketConfig.L1_MARKET_DATA_PREFIX;

@Log4j2
@Component
public class MarketDataConsumer {

    @Autowired
    private CounterConfig config;

    //<code, 最新的五档行情>
    private Map<String, L1MarketData> l1Cache = new HashMap<>();

    @PostConstruct
    private void init() {
        EventBus eventBus = config.getVertx().eventBus();
        //处理撮合核心发送过来的数据
        eventBus.consumer(INNER_MARKET_DATA_CACHE_ADDR)
                .handler(buffer -> {
                    Buffer body = (Buffer) buffer.body();
                    if(body.length() == 0){
                        log.error("market data is empty");
                        return;
                    }
                    L1MarketData[] marketData = null;

                    try {
                        marketData = config.getByteCodec().deseriallize(body.getBytes(), L1MarketData[].class);
                    } catch (Exception e) {
                        log.error("decode market data error:{}",e);
                    }

                    if(ArrayUtils.isEmpty(marketData)){
                        log.error("market data is empty");
                        return;
                    }

                    for(L1MarketData md : marketData) {
                        L1MarketData data = l1Cache.get(md.code);
                        if(data == null || data.timestamp < md.timestamp){
                            l1Cache.put(md.code,md);
                        }else{
                            log.error("market data is old:{}",md);
                        }
                    }
                });
        //委托终端的行情请求的处理器
        eventBus.consumer(L1_MARKET_DATA_PREFIX)
                .handler(h -> {
                   //只要是同一个vertx产生的eventbus，我们不用显式区分，都是通用的
                    String code = h.headers().get("code");
                    L1MarketData data = l1Cache.get(code);
                    h.reply(JsonUtil.toJson(data));
                });
    }

}
