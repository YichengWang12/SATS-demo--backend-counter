package com.ethan.counter.service;

import com.ethan.counter.bean.res.Account;

public interface AccountService {
    //login

    /**
     *
     * @param uid
     * @param password
     * @param captcha
     * @param captchaId
     * @return
     * @throws Exception
     */
    Account login(long uid, String password, String captcha, String captchaId) throws Exception;

    //if cache has info indicates user was login

    /**
     *
     * @param token
     * @return
     */
    boolean accountExistInCache(String token);

    boolean logout(String token);

    boolean updatePassword(long uid, String oldPassword, String newPassword);
}
