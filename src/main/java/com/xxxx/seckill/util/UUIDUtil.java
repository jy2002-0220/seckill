package com.xxxx.seckill.util;

import java.util.UUID;

public class UUIDUtil {
    public static String uuId(){
        return UUID.randomUUID().toString().replace("-","");
    }

    public static void main(String[] args) {
        System.out.println(uuId());
    }
}
