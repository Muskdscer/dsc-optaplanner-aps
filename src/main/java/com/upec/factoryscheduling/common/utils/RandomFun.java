package com.upec.factoryscheduling.common.utils;

import java.util.UUID;


public class RandomFun {

    private static RandomFun uniqueInstance = null;
    private static long flag = 0;

    private RandomFun() {

    }

    public static RandomFun getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new RandomFun();
            flag = System.currentTimeMillis();
        }
        return uniqueInstance;
    }

    public String getRandom() {
        flag = (flag < System.currentTimeMillis()) ? (System.currentTimeMillis()) : (flag + 1);
        return String.valueOf(flag);
    }


    public String getUuid() {
        long uuid = UUID.randomUUID().getMostSignificantBits();
        return String.valueOf(uuid);
    }
}
