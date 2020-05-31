package com.fandou.learning.netty.core.chapter5.rpc.provider;

import com.fandou.learning.netty.core.chapter5.rpc.api.CalcService;

public class SimpleCalcServiceImpl implements CalcService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int sub(int a, int b) {
        return a - b;
    }

    @Override
    public int mult(int a, int b) {
        return a * b;
    }

    @Override
    public int div(int a, int b) {
        return a / b;
    }
}
