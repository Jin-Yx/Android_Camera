package com.jinyx.camera.utils;

import java.math.BigDecimal;

public class BigDecimalUtils {

    public static double jia(double a, double b) {
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.add(bd2).doubleValue();
    }

    public static double jian(double a, double b) {
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.subtract(bd2).doubleValue();
    }

    public static double cheng(double a, double b) {
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.multiply(bd2).doubleValue();
    }

    public static double chu(double a, double b) {
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.divide(bd2, 10, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static int compare(double a, double b) {
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.compareTo(bd2);
    }

}
