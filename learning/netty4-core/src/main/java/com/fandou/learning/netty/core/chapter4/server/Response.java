package com.fandou.learning.netty.core.chapter4.server;

public interface Response {
    public void write(String message) throws Exception;
}
