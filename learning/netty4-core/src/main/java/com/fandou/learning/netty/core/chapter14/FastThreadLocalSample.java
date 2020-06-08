package com.fandou.learning.netty.core.chapter14;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.Data;

/**
 * 验证Netty的FastThreadLocal线程内部类
 */
@Data
public class FastThreadLocalSample {
    /**
     * Netty的线程内部变量类实例，用于存储可在相同线程或需要在相同线程内共享的内容，功能类似ThreadLocal，但性能比ThreadLocal要好
     */
    private final FastThreadLocal fastThreadLocal;

    public FastThreadLocalSample() {
        fastThreadLocal = new SimpleFastThreadLocal();
    }

    final class SimpleFastThreadLocal extends FastThreadLocal<ThreadObject>{
        /**
         * 创建应用程序需要的可在线程内共享的对象
         * 在第一次调用get方法时，如果没有设置过值，将调用次方法，相当于首次调用FastThreadLocal的set方法
         *
         * @return
         * @throws Exception
         */
        @Override
        protected ThreadObject initialValue() throws Exception {
            // 仅作演示
            return new ThreadObject();
        }
    }

    /**
     * 线程相关信息的对象，演示用
     */
    static final class ThreadObject {
        private final long createdTime;
        private final String threadName;

        public ThreadObject() {
            threadName = Thread.currentThread().getId() + ":" + Thread.currentThread().getName();
            createdTime = System.nanoTime();
        }

        @Override
        public String toString() {
            return "{" + threadName + " -> " + createdTime + "}";
        }
    }

    public static void main(String[] args) {
        FastThreadLocalSample holder = new FastThreadLocalSample();
        System.out.println("主线程" + Thread.currentThread().getName() + "开始运行...");

        // 新开第一个线程，循环遍历，每次隔1秒设置runner中的对象
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 线程启动，获取当前线程下，设置的线程内共享对象的实例
                Object o = holder.fastThreadLocal.get();

                try{
                    // 遍历10，每次修改后，重新获取最新的值与原来的比较是否相同，
                    // 中间间隔1秒，让第二个线程有机会运行并获取修改后对象实例并进行比较
                    for (int i = 0; i < 10; i++) {
                        holder.fastThreadLocal.set(new ThreadObject());
                        Object obj = holder.fastThreadLocal.get();
                        // 每次修改后，与最开始的值进行比较，因为是相同线程下的修改，结果均为false
                        System.out.println("在线程" + Thread.currentThread().getName() + "中修改runner中的对象 => " + (o == obj) + ",obj => " + obj);
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
                    // 线程启动，获取当前线程下，设置的线程内共享对象的实例
                    Object o = holder.fastThreadLocal.get();

                    // 遍历10，每次与原来的比较是否相同，中间间隔1秒，让第一个线程有机会再次运行修改操作
                    for (int i = 0; i < 10; i++) {
                        Object obj = holder.fastThreadLocal.get();
                        // 由于当前线程下没有做任何的修改操作，获得的对象都是相同的，所以每次比较都为true
                        System.out.println("在线程" + Thread.currentThread().getName() + "中比较runner中的对象 => " + (o == obj) + ",obj => " + obj);
                        Thread.sleep(1000L);
                    }
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();

        // 结论，不同线程下，设置的线程内共享对象，其值是不相同的，对共享对象的修改的影响仅限线程内，不会影响其它线程
        // FastThreadLocal内部使用数组实现线程内共享变量的存储，每个FastThreadLocal对应一个不可变的索引index，
        // 而这个数组绑定线程（Netty抽象的FastThreadLocalThread线程或存储在JDK实现线程隔离的ThreadLocal实例）中，
        // 即每个线程绑定1个数组，这个数组必要时会扩容。在调用get方法时，FastThreadLocal会使用持有的index去归属当前线程的数组中检索，
        // 如果不存在，则调用最终initialValue方法初始初始化一个对象实例，由于被实例重载了该方法，所以，不同线程调用get方法时，
        // 虽然时同一个FastThreadLocal实例，相同的索引index，但由于用来存取共享变量的数组不一样，所以会返回不同的对象。
        // 如果是相同线程，则get/set方法操作的将保证是同一个对象（严格说是操作数组上同一个位置的对象）。
    }
}
