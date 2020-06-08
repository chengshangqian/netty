package com.fandou.learning.netty.core.chapter14;

import org.junit.jupiter.api.Test;

class FastThreadLocalSampleTest {
    @Test
    void testFastThreadLocalRunner(){
        FastThreadLocalSample holder = new FastThreadLocalSample();

        // 新开第一个线程，循环遍历，每次隔1秒设置runner中的对象
        new Thread(new Runnable() {
            @Override
            public void run() {
                Object o = holder.getFastThreadLocal().get();
                try{
                    for (int i = 0; i < 10; i++) {
                        holder.getFastThreadLocal().set(new FastThreadLocalSample.ThreadObject());
                        System.out.println("在线程" + Thread.currentThread().getName() + "中修改runner中的对象...");
                        Thread.sleep(1000L);
                    }
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();

        // 新开第二个线程，循环遍历，每次隔1秒获取runner中的对象
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Object obj = holder.getFastThreadLocal().get();
                    for (int i = 0; i < 10; i++) {
                        System.out.println("在线程" + Thread.currentThread().getName() + "中比较runner中的对象 => " + (obj == holder.getFastThreadLocal().get()));
                        Thread.sleep(1000L);
                    }
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();
    }
}