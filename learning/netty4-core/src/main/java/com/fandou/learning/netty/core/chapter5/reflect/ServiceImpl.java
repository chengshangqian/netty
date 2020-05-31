package com.fandou.learning.netty.core.chapter5.reflect;

public class ServiceImpl implements Service {
    private String key;
    protected int limit;
    public static String code;

    public ServiceImpl(){
        this.key = "你好！";
        this.limit = Integer.MAX_VALUE;
    }

    public ServiceImpl(String key){
        this.key = key;
        this.limit = Integer.MAX_VALUE;
    }

    @Override
    public void process(int max) {
        for (int i = 0; i < max; i++) {
            System.out.println(key);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static String getCode() {
        return code;
    }

    public static void setCode(String code) {
        ServiceImpl.code = code;
    }

    public int getLimit() {
        return limit;
    }

    protected void setLimit(int limit) {
        this.limit = limit;
    }

    private void init(){
        System.out.println("初始化方法init被调用.");
    }

    public static void main(String[] args) {
        for(String arg : args){
            System.out.print(arg + " ");
        }
        System.out.println();
    }

    public static String append (int a,String b){
        return a + "-" + b;
    }
}
