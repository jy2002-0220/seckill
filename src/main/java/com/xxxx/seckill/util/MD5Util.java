package com.xxxx.seckill.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    private static final String salt = "1a2b3c4d";

    //MD5加密
    public static String md5(String s) {
        return DigestUtils.md5Hex(s);
    }

    //前端来的密码混合盐加密
    public static String inputPassToFromPass(String inputPasss) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPasss + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    public static void main(String[] args) {
        String s = "123456";
        String s1 = inputPassToFromPass(s);
        System.out.println(s1);
    }

}
