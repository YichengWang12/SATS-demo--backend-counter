package com.ethan.counter.util;
import com.ethan.counter.bean.res.Account;
import com.ethan.counter.bean.res.OrderInfo;
import com.ethan.counter.bean.res.PosiInfo;
import com.ethan.counter.bean.res.TradeInfo;
import com.ethan.counter.cache.CacheType;
import com.ethan.counter.cache.RedisStringCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import thirdpart.hq.MatchData;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderStatus;

import java.util.List;
import java.util.Map;

@Component
public class DbUtil {

    private static DbUtil dbUtil= null;

    private DbUtil(){
    }

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    public static void saveTrade(int counterId, MatchData md, OrderCmd orderCmd) {
        if(orderCmd == null){
            return;
        }
        Map<String,Object> param = Maps.newHashMap();
        param.put("Id",md.tid);
        param.put("UId",orderCmd.uid);
        param.put("Code",orderCmd.code);
        param.put("Direction",orderCmd.direction.getDirection());
        param.put("Price",md.price);
        param.put("TCount",md.volume);
        param.put("OId",counterId); // 高位为counterId
        param.put("Date",TimeformatUtil.yyyyMMdd(md.timestamp));
        param.put("Time",TimeformatUtil.hhMMss(md.timestamp));
        dbUtil.getSqlSessionTemplate().insert("orderMapper.saveTrade",param);
        //更新缓存
        RedisStringCache.remove(Long.toString(orderCmd.uid), CacheType.TRADE);
    }

    public static void updateOrder(long uid, int oid, OrderStatus finalStatus) {
        Map<String,Object> param = Maps.newHashMap();
        param.put("Id",oid);
        param.put("Status",finalStatus.getCode());
        dbUtil.getSqlSessionTemplate().update("orderMapper.updateOrder",param);
        //更新缓存
        RedisStringCache.remove(Long.toString(uid), CacheType.ORDER);
    }

    private void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate){
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    private SqlSessionTemplate getSqlSessionTemplate(){
        return this.sqlSessionTemplate;
    }

    @PostConstruct
    private void init(){
        dbUtil = new DbUtil();
        dbUtil.setSqlSessionTemplate(this.sqlSessionTemplate);
    }

    public static long getId(){
        Long res = dbUtil.getSqlSessionTemplate().selectOne("testMapper.queryBalance");
        if(res == null){
            return -1;
        }else{
            return 0;
        }
    }

    //identity authorization
    public static Account queryAccount(long uid, String password){
        return dbUtil.getSqlSessionTemplate().selectOne(
                "userMapper.queryAccount",
                ImmutableMap.of("Uid",uid,"Password",password)
        );
    }

    public static void updateLoginTime(long uid, String nowDate, String nowTime){
        dbUtil.getSqlSessionTemplate().update(
                "userMapper.queryAccount",
                ImmutableMap.of(
                        "Uid",uid,
                        "ModifyDate",nowDate,
                        "ModifyTime",nowTime
                )
        );
    }

    public static int updatePassword(long uid, String oldPassword, String newPassword){
        return dbUtil.getSqlSessionTemplate().update(
                "userMapper.updatePassword",
                ImmutableMap.of(
                        "Uid",uid,
                        "OldPassword",oldPassword,
                        "NewPassword",newPassword
                )
        );
    }

    ////////////////////////////balance////////////////////////
    public static long getBalance (long uid){
        Long res = dbUtil.getSqlSessionTemplate().selectOne("orderMapper.queryBalance",
                ImmutableMap.of("Uid",uid));
        if(res == null){
            return -1;
        }else{
            return res;
        }
    }


    public static void addBalance(long uid, long balance){
        dbUtil.getSqlSessionTemplate().update("orderMapper.updateBalance",ImmutableMap.of("Uid",uid,"Balance",balance));
    }

    public static void reduceBalance(long uid, long balance){
        addBalance(uid,-balance);
    }

    //////////////////////////position/////////////////////////
    public static List<PosiInfo> getPosiList(long uid){
        // query cache
        String suid = Long.toString(uid);
        String posiS = RedisStringCache.get(suid, CacheType.POSI);
        if(StringUtils.isEmpty(posiS) || "[]".equals(posiS)){
            //no cache, query db, update cache
            List<PosiInfo> temp = dbUtil.getSqlSessionTemplate().selectList(
                    "orderMapper.queryPosi",
                    ImmutableMap.of("Uid",uid)
            );
            List<PosiInfo> posiInfos = CollectionUtils.isEmpty(temp)? Lists.newArrayList() : temp;
            //update cache
            RedisStringCache.cache(suid,JsonUtil.toJson(posiInfos),CacheType.POSI);
            return posiInfos;
        }
        else{
            //cache

            return JsonUtil.fromJsonArr(posiS,PosiInfo.class);
        }
    }

    public static PosiInfo getPosi(long uid, String code){
        return dbUtil.getSqlSessionTemplate().selectOne("orderMapper.queryPosi",ImmutableMap.of("Uid",uid,"Code",code));
    }

    public static void addPosi(long uid,String code,long volume,long price){
        // if position exists
        PosiInfo posiInfo = getPosi(uid,code);
        if(posiInfo == null){
            // add new one
            insertPosi(uid,code,volume,price);
        }else{
            // update exists
            posiInfo.setCount(posiInfo.getCount()+volume);
            posiInfo.setCost(posiInfo.getCost() + price * volume);
//            if(posiInfo.getCount() == 0){
//                deletePosi(posi);
//            }else{
                updatePosi(posiInfo);
            //}
        }
    }

    private static void updatePosi(PosiInfo posiInfo) {
        dbUtil.getSqlSessionTemplate().insert("orderMapper.updatePosi",
                ImmutableMap.of("Uid",posiInfo.getUid(),"Code",posiInfo.getCode(),"Count",posiInfo.getCount(),"Cost",posiInfo.getCost()));
    }


    private static void insertPosi(long uid, String code, long volume, long price) {
        dbUtil.getSqlSessionTemplate().insert("orderMapper.insertPosi",
                ImmutableMap.of("Uid",uid,"Code",code,"Count",volume,"Cost",price * volume));
    }

    public static void reducePosi(long uid,String code,long volume,long price){
        addPosi(uid,code,-volume,price);
    }

    ///////////////////////////////order///////////////////////
    public static List<OrderInfo> getOrderList(long uid, String date, String code){
        // query cache
        String suid = Long.toString(uid);
        String orderS = RedisStringCache.get(suid, CacheType.ORDER);

        if(StringUtils.isEmpty(orderS) || "[]".equals(orderS)){

            //no cache, query db, update cache
            List<OrderInfo> temp = dbUtil.getSqlSessionTemplate().selectList(
                    "orderMapper.queryOrder",
                    ImmutableMap.of("Uid",uid,
                            "date",date,
                            "code",code
                    )
            );
            List<OrderInfo> orderInfos = CollectionUtils.isEmpty(temp)? Lists.newArrayList() : temp;
            //update cache
            RedisStringCache.cache(suid,JsonUtil.toJson(orderInfos),CacheType.ORDER);
            return orderInfos;
        }
        else{
            //cache
            return JsonUtil.fromJsonArr(orderS,OrderInfo.class);
        }
    }

    //trade
    public static List<TradeInfo> getTradeList(long uid,String date,String code)
    {
        // query cache
        String suid = Long.toString(uid);
        String tradeS = RedisStringCache.get(suid, CacheType.TRADE);

        if(StringUtils.isEmpty(tradeS) || "[]".equals(tradeS)){
            //no cache, query db, update cache
            List<TradeInfo> temp = dbUtil.getSqlSessionTemplate().selectList(
                    "orderMapper.queryTrade",
                    ImmutableMap.of("Uid",uid,"date",date)
            );
            List<TradeInfo> tradeInfos = CollectionUtils.isEmpty(temp)? Lists.newArrayList() : temp;
            //update cache
            RedisStringCache.cache(suid,JsonUtil.toJson(tradeInfos),CacheType.TRADE);
            return tradeInfos;
        }
        else{
            //cache
            return JsonUtil.fromJsonArr(tradeS,TradeInfo.class);
        }
    }

    //query stock info (for autocomplete)
    public static List<Map<String,Object>> queryAllStockInfo(){
        //System.out.println(dbUtil.getSqlSessionTemplate().selectList("stockMapper.queryStock"));
        return dbUtil.getSqlSessionTemplate().selectList("stockMapper.queryStock");
    }

    ///////////////////////////////order operation//////////////////////////////
    public static int saveOrder(OrderCmd orderCmd){
        Map<String,Object> param = Maps.newHashMap();
        param.put("Uid",orderCmd.uid);
        param.put("Code",orderCmd.code);
        param.put("Direction",orderCmd.direction.getDirection());
        param.put("Type",orderCmd.orderType.getType());
        param.put("Price",orderCmd.price);
        param.put("Count",orderCmd.volume);
        param.put("Tcount",0);
        param.put("Status",OrderStatus.NOT_SET.getCode());
        param.put("Date",TimeformatUtil.yyyyMMdd(orderCmd.timestamp));
        param.put("Time",TimeformatUtil.hhMMss(orderCmd.timestamp));

        int count = dbUtil.getSqlSessionTemplate().insert(
                "orderMapper.saveOrder",param
        );
        //success
        if(count > 0){
            return Integer.parseInt(param.get("ID").toString());
        }
        else{
            return -1;
        }
    }
}
