package com.ethan.counter.cache;

import com.ethan.counter.bean.res.StockInfo;
import com.ethan.counter.util.DbUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class StockCache {
    //Map<String,List<StockInfo>>
    // A - > AAPL, AAAA ......
    private HashMultimap<String, StockInfo> invertIndex = HashMultimap.create();

    public Collection<StockInfo> getStock(String key){
        return invertIndex.get(key);
    }

    @PostConstruct
    private void createInvertIndex(){
        log.info("load stock info from db");
        long st = System.currentTimeMillis();

        //1. load stock info from db
        List<Map<String,Object>> res = DbUtil.queryAllStockInfo();

        if(CollectionUtils.isEmpty(res)){
            log.error("failed to load stock: empty");
            return;
        }

        //2. establish invert index
        for(Map<String,Object>r : res){
            String code = r.get("code").toString();
            String name = r.get("name").toString();
            String abbrName = r.get("abbrName").toString();

            StockInfo stock = new StockInfo(code,name,abbrName);


            List<String> codeMetas = splitData(code);
            List<String> abbrNameMetas = splitData(abbrName);

            codeMetas.addAll(abbrNameMetas);
//            log.info(codeMetas);
            for(String key : codeMetas){
                //limit length of index list
                Collection<StockInfo> stockInfos = invertIndex.get(key);
                if(!CollectionUtils.isEmpty(stockInfos) && stockInfos.size() >= 10){
                    continue;
                }
                else{
                    invertIndex.put(key,stock);
                }
            }
        }
        log.info("Current invertIndex contents: " + invertIndex);
        log.info("loaded,used " + (System.currentTimeMillis() - st) + "ms");
    }

    private List<String> splitData(String code) {
        // Split string into all subsets
        // Apple -> A Ap App Appl Apple
        // p pp ppl pple
        // p pl ple
        // l le
        // e
        List<String> list = Lists.newArrayList();
        int outLen = code.length();
        for(int i =0; i< outLen;i++){
            for(int j = i+1;j<=outLen;j++){
                list.add(code.substring(i,j));
            }
        }
        return list;
    }
}
