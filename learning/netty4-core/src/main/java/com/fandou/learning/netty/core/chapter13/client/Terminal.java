package com.fandou.learning.netty.core.chapter13.client;

/**
 * 终端类型
 */
public enum Terminal {
    CONSOLE("Console"),
    WEB_SOCKET("WebSocket");

    private String name;

    Terminal(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
