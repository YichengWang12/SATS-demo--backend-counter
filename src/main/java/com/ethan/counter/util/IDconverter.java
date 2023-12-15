package com.ethan.counter.util;

public class IDconverter {
    public static long combineIntToLong(int high, int low){
        return ((long) high << 32 & 0xFFFFFFFF00000000L) | ((long) low & 0xFFFFFFFFL);
    }

    public static int[] splitLongToInt(long val){
        int[] res = new int[2];
        res[1] = (int) (0xFFFFFFFFL & val); // low
        res[0] = (int) ((0xFFFFFFFF00000000L & val) >> 32);
        return res;
    }
}
