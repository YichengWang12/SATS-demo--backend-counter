package com.ethan.counter.service;

import com.ethan.counter.bean.res.Account;
import com.ethan.counter.cache.CacheType;
import com.ethan.counter.cache.RedisStringCache;
import com.ethan.counter.util.DbUtil;
import com.ethan.counter.util.JsonUtil;
import com.ethan.counter.util.TimeformatUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import thirdpart.uuid.EthanUuid;

import java.util.Date;

@Component
public class AccountServiceImpl implements AccountService {
    @Override
    public Account login(long uid, String password, String captcha, String captchaId) throws Exception {
        // 1.parameters validation check
        if(StringUtils.isAnyBlank(password,captcha,captchaId)){
            return null;
        }
        // 2.captcha check
        String captchaCache =  RedisStringCache.get(captchaId, CacheType.CAPTCHA);
        if(StringUtils.isEmpty(captchaCache)){
            return null;
        }else if(!StringUtils.equalsIgnoreCase(captcha,captchaCache)){
            return null;
        }
        RedisStringCache.remove(captchaId,CacheType.CAPTCHA);
        // 3.uid & password check
        Account account  = DbUtil.queryAccount(uid,password);
        if(account == null){
            return null;
        }else{
            //add unique id as identify tool
            account.setToken(String.valueOf(EthanUuid.getInstance().getUUID()));

            //cache
            RedisStringCache.cache(String.valueOf(account.getToken()), JsonUtil.toJson(account),CacheType.ACCOUNT);

            //update lastLoginTime
            Date date = new Date();
            DbUtil.updateLoginTime(uid, TimeformatUtil.yyyyMMdd(date),TimeformatUtil.hhMMss(date));
            return account;
        }
    }

    @Override
    public boolean accountExistInCache(String token) {
        if(StringUtils.isBlank(token)){
            return false;
        }

        // check cache
        String acc = RedisStringCache.get(token,CacheType.ACCOUNT);
        if(acc != null){
            RedisStringCache.cache(token,acc,CacheType.ACCOUNT);
            return true;
        }else{
            return false;
        }
    }


    //remove token in cache when logout
    @Override
    public boolean logout(String token) {
        RedisStringCache.remove(token,CacheType.ACCOUNT);
        return true;
    }

    @Override
    public boolean updatePassword(long uid, String oldPassword, String newPassword) {
        int res = DbUtil.updatePassword(uid,oldPassword,newPassword);
        return res != 0;
    }
}
