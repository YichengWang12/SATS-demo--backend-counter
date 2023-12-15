package com.ethan.counter.bean;


import com.ethan.counter.config.CounterConfig;
import com.ethan.counter.util.DbUtil;
import com.ethan.counter.util.IDconverter;
import com.ethan.counter.util.JsonUtil;
import com.google.common.collect.ImmutableMap;
import io.netty.util.collection.LongObjectHashMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import thirdpart.hq.MatchData;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderDirection;
import thirdpart.order.OrderStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ethan.counter.bean.MqttBusConsumer.INNER_MATCH_DATA_ADDR;
import static com.ethan.counter.config.WebSocketConfig.TRADE_NOTIFY_ADDR_PREFIX;

@Log4j2
@Component
public class MatchDataConsumer {
    public static final String ORDER_DATA_CACHE_ADDR = "order_data_cache_addr";

    @Autowired
    private CounterConfig config;

    //<委托编号，OrderCmd>
    private LongObjectHashMap<OrderCmd> orderCmdMap = new LongObjectHashMap<>();

    @PostConstruct
    private void init() {
        EventBus eventBus = config.getVertx().eventBus();

        eventBus.consumer(INNER_MATCH_DATA_ADDR)
                .handler(buffer -> {
                    //数据长度判断
                    Buffer body = (Buffer) buffer.body();
                    if (body.length() == 0) {
                        log.error("match data is empty");
                        return;
                    }
                    MatchData[] matchData = null;

                    try {
                        matchData = config.getByteCodec().deseriallize(body.getBytes(), MatchData[].class);
                    } catch (Exception e) {
                        log.error("decode match data error:{}", e);
                    }

                    if (ArrayUtils.isEmpty(matchData)) {
                        log.error("match data is empty");
                        return;
                    }

                    //可能出现同一笔委托有多笔matchdata 需要分类
                    //按照oid进行分类
                    Map<Long, List<MatchData>> collect = Arrays.asList(matchData)
                            .stream().collect(Collectors.groupingBy(t -> t.oid));

                    //遍历map
                    for (Map.Entry<Long, List<MatchData>> entry : collect.entrySet()) {
                        if (CollectionUtils.isEmpty(entry.getValue())) {
                            continue;
                        }
                        //拆分柜台编号和委托id
                        long oid = entry.getKey();
                        int counterId = IDconverter.splitLongToInt(oid)[1];

                        //修改数据库和通知委托终端
                        updateAndNotify(counterId, entry.getValue(), orderCmdMap.get(oid));


                    }
                });
    }

    private void updateAndNotify(int counterId, List<MatchData> value, OrderCmd orderCmd) {
        if (CollectionUtils.isEmpty(value)) {
            return;
        }
        //成交 : 如果一个委托有多笔成交数据， 它们都要被存下来
        for (MatchData md : value) {
            OrderStatus status = md.status;
            if (status == OrderStatus.TRADE_ED || status == OrderStatus.PART_TRADE) {
                //更新数据库
                DbUtil.saveTrade(counterId, md, orderCmd);
                //持仓资金多退少补 要根据买卖方向判断
                if (orderCmd.direction == OrderDirection.BUY) {
                    // 当有一部分成交价格低于委托价格，需要释放提前锁住的多余的资金
                    if (orderCmd.price > md.price) {
                        DbUtil.addBalance(orderCmd.uid, md.volume * (orderCmd.price - md.price));
                    }
                    DbUtil.addPosi(orderCmd.uid, orderCmd.code, md.volume, md.price);
                } else if (orderCmd.direction == OrderDirection.SELL) {
                    //这里只需要对资金做出改变，因为持仓在提交委托时就变化, 在实际成交时，还需要考虑税费，佣金等等
                    DbUtil.addBalance(orderCmd.uid, md.volume * md.price);
                } else {
                    log.error("unknown order direction:{}", orderCmd.direction);

                }

                // 通知客户端:发生成交时
                // 参数 要通知的地址
                config.getVertx().eventBus().publish(
                        TRADE_NOTIFY_ADDR_PREFIX + orderCmd.uid,
                        JsonUtil.toJson(
                                ImmutableMap.of(
                                        "code", orderCmd.code,
                                        "direction", orderCmd.direction,
                                        "volume", md.volume
                                )
                        )
                );

            }
        }
        //委托变动
        //根据最后一个matchdata的状态处理
        MatchData finalMatchData = value.get(value.size() - 1);
        OrderStatus finalStatus = finalMatchData.status;
        DbUtil.updateOrder(orderCmd.uid, counterId, finalStatus);

        //更新完后，需要考虑撤单这类委托对于持仓和资金的影响
        if (finalStatus == OrderStatus.CANCEL_ED || finalStatus == OrderStatus.PART_CANCEL) {
            //既然最后一笔有撤单的动作，委托的缓存里就不需要有这笔数据了
            orderCmdMap.remove(orderCmd.oid);
            //根据委托的不同方向进行处理
            if (orderCmd.direction == OrderDirection.BUY) {
                //如果是买单，那么需要退回冻结的资金
                DbUtil.addBalance(orderCmd.uid, -1 * (orderCmd.price * finalMatchData.volume));
            } else if (orderCmd.direction == OrderDirection.SELL) {
                //如果是卖单，那么需要退回冻结的持仓
                DbUtil.addPosi(orderCmd.uid, orderCmd.code, -1 * finalMatchData.volume, orderCmd.price);
            } else {
                log.error("unknown order direction:{}", orderCmd.direction);
            }

        }
        // 通知客户端:委托变动时
        config.getVertx().eventBus().publish(
                TRADE_NOTIFY_ADDR_PREFIX + orderCmd.uid,
                ""//实际的信息由前端通过接口查询
        );
    }


}
