package com.fandou.learning.netty.core.chapter5.rpc.api;

/**
 * 计算服务
 */
public interface CalcService {
    /**
     * 加法：计算两个整数相加和，a+b
     *
     * @param a
     * @param b
     * @return
     */
    public int add(int a,int b);

    /**
     * 减法：计算两个整数相减，a-b
     *
     * @param a
     * @param b
     * @return
     */
    public int sub(int a,int b);

    /**
     * 乘法：计算两个整数相乘，a*b
     *
     * @param a
     * @param b
     * @return
     */
    public int mult(int a,int b);

    /**
     * 除法：计算两个整数相除，a / b
     *
     * @param a
     * @param b
     * @return
     */
    public int div(int a,int b);
}
