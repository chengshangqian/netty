package com.fandou.learning.netty.core.chapter14;

import io.netty.util.Recycler;

/**
 * 回收栈的使用示例，也是线程线程相关的
 */
public class ReCyclerSample {

    private static final Recycler<User> RECYCLER = new Recycler<User>(){
        @Override
        protected User newObject(Handle<User> handle) {
            return new User(handle);
        }
    };

    private static class User {
        // 回收栈持有者
        private final Recycler.Handle<User> handle;

        public User(Recycler.Handle<User> handle) {
            this.handle = handle;
        }

        /**
         * 放入回收栈
         */
        public void recycle(){
            // 入栈
            handle.recycle(this);
        }
    }

    public static void main(String[] args) {
        // 首次获取，没有，创建新的User对象实例
        User peter = RECYCLER.get();
        // 放入User对象的回收栈
        peter.recycle();

        // 栈中已经有回收可用的User对象，直接返回
        User lucy = RECYCLER.get();
        // 放入User对象的回收栈
        lucy.recycle();

        System.out.println(peter == lucy);
    }


}
