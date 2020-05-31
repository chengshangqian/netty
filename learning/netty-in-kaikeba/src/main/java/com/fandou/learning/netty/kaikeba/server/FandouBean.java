package com.fandou.learning.netty.kaikeba.server;

/**
 * 自定义的bean对象
 */
public class FandouBean {
    // 请求源(应用)
    // 0xA 表示来自A应用, 0xB表示来自B应用
    private byte application;

    // 请求类别
    // 0xA 表示订单, 0xB表示用户, 0xC表示心跳检测
    private byte category;

    // 消息长度
    private int length;

    // 请求服务地址
    private String service;

    public FandouBean(byte application,byte category,int length,String service){
        this.application = application;
        this.category = category;
        this.length = length;
        this.service = service;
    }

    public byte getApplication() {
        return application;
    }

    public byte getCategory() {
        return category;
    }

    public int getLength() {
        return length;
    }

    public String getService() {
        return service;
    }

    public String toString(){
        return "{app:" + application + ",category:" + category + ",length:" + length + ",service:" + service + "}";
    }
}
