package com.fandou.learning.netty.action.chapter13;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

/**
 * 日志广播器
 */
public class LogEventBroadcaster {
    /**
     * 事件循环组
     */
    private final EventLoopGroup group;

    /**
     * 日志广播器引导
     */
    private final Bootstrap bootstrap;

    /**
     * 日志文件
     */
    private final File file;

    /**
     * 初始化日志广播器
     *
     * @param remoteAddress 远程主机地址
     * @param file
     */
    public LogEventBroadcaster(InetSocketAddress remoteAddress,File file) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                // 使用异步的无连接数据报Channel
                .channel(NioDatagramChannel.class)
                // 使用广播模式
                .option(ChannelOption.SO_BROADCAST,true)
                // 添加日志编码器
                .handler(new LogEventEncoder(remoteAddress));

        // 初始化日志文件
        this.file = file;
    }

    /**
     * 启动日志广播
     *
     * @throws Exception
     */
    public void run() throws Exception {
        // 绑定端口0是广播的意思?
        Channel channel = bootstrap.bind(0).sync().channel();
        long pointer = 0;

        // 获取日志文件名：完整路径
        String logfile = file.getAbsolutePath();

        // 监控日志文件内容的变化，如果有新的内容读取并发送/广播
        for(;;){
            // 获取文件大小
            long len = file.length();

            // 如果当前文件大小发生变化，文件被还原为以前的内容或重置，将指针指向当前文件大小
            if(len < pointer){
                pointer = len;
            }
            // 如果当前文件大小发生变化，产生新的日志内容
            else if(len > pointer){
                // 读取新的日志内容
                RandomAccessFile raf = new RandomAccessFile(file,"r");
                raf.seek(pointer);

                // 按行读取新的日志内容
                String msg = null;
                while((msg = raf.readLine()) != null){
                    // 解决可能的中文乱码：readLine方法读取的内容，不论原来编码是什么，都会以ISO-8859-1读取
                    msg = new String(msg.getBytes("ISO-8859-1"),"UTF-8");

                    // 将日志信息写入到channel中
                    channel.writeAndFlush(new LogEvent(logfile,msg));
                }

                // 更新指针位置
                pointer = raf.getFilePointer();
                // 关闭RandomAccessFile
                raf.close();
            }

            try{
                // 睡眠1秒，然后重新检查是否有新的日志内容产生。如果等待过程被异常中断，退出循环。
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }
        }
    }

    /**
     * 关闭日志广播器资源
     */
    public void stop(){
        group.shutdownGracefully();
    }
}
