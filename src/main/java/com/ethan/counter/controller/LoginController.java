package com.ethan.counter.controller;

import com.ethan.counter.bean.res.Account;
import com.ethan.counter.bean.res.CaptchaRes;
import com.ethan.counter.bean.res.CounterRes;
import com.ethan.counter.cache.CacheType;
import com.ethan.counter.cache.RedisStringCache;
import com.ethan.counter.service.AccountService;
import com.ethan.counter.util.Captcha;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thirdpart.uuid.EthanUuid;

import static com.ethan.counter.bean.res.CounterRes.*;

@RestController
@RequestMapping("/sign-in")
@Log4j2
public class LoginController {


    @RequestMapping("/captcha")
    public CounterRes captcha() throws Exception{
        //1.generate
        Captcha captcha = new Captcha(120,40,4,10);

        //2.put captcha<id,value> into cache
        String uuid = String.valueOf(EthanUuid.getInstance().getUUID());
        RedisStringCache.cache(uuid,captcha.getCode(), CacheType.CAPTCHA);

        //3.return to frontend in base64
        //uuid,base 64
        CaptchaRes res = new CaptchaRes(uuid,captcha.getBase64ByteStr());
        return new CounterRes(res);
    }

    @Autowired
    private AccountService accountService;

    @RequestMapping("/usersignin")
    public CounterRes login(@RequestParam long uid,
                            @RequestParam String password,
                            @RequestParam String captcha,
                            @RequestParam String captchaId) throws Exception{
        Account account = accountService.login(uid,password,captcha,captchaId);

        if(account == null){
            return new CounterRes(FAIL,"uid/password is invalid",null);
        }else{
            return new CounterRes(account);
        }
    }

    @RequestMapping("/sign-in-fail")
    public CounterRes loginFail(){
        return new CounterRes(RELOGIN,"Please sign in again",null);
    }

    @RequestMapping("/signout")
    public CounterRes logout(@RequestParam String token){
        accountService.logout(token);
        return new CounterRes(SUCCESS,"Sign out successfully",null);
    }

    @RequestMapping("/updatepassword")
    public CounterRes updatePassword(@RequestParam int uid,
                                     @RequestParam String oldPassword,
                                     @RequestParam String newPassword){
        boolean res =accountService.updatePassword(uid,oldPassword,newPassword);
        if(res){
            return new CounterRes(SUCCESS,"Operation Successful",null);
        }
        else{
            return new CounterRes(FAIL,"Operation Failed",null);
        }
    }
}
