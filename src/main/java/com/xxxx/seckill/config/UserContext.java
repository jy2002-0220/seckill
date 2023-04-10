package com.xxxx.seckill.config;

import com.xxxx.seckill.pojo.User;

public class UserContext {
    private static ThreadLocal<User> userHolder = new ThreadLocal<User>();
    public static void setUser(User user){//创建
        userHolder.set(user);
    }
    public static User getUser(){//获取User
        return userHolder.get();
    }
}
