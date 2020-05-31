package com.fandou.learning.netty.action.chapter13;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * LogEvent-DatagramPacket编码器
 */
public class LogEventEncoder extends MessageToMessageEncoder<LogEvent> {
    /**
     * 远程主机地址：即接收数据报的远程主机
     */
    private final InetSocketAddress remoteAddress;

    /**
     * 初始化远程主机地址
     *
     * @param remoteAddress
     */
    public LogEventEncoder(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * 将LogEvent编码为DatagramPacket
     *
     * @param ctx
     * @param logEvent
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, LogEvent logEvent, List<Object> out) throws Exception {
        // 打印
        //System.out.println(logEvent.getLogfile() + ";" + logEvent.getMsg());

        // 日志文件名/路径
        byte[] file = logEvent.getLogfile().getBytes(CharsetUtil.UTF_8);

        // 发送的消息即日志内容（一行）
        byte[] msg = logEvent.getMsg().getBytes(CharsetUtil.UTF_8);

        // 创建ByteBuf，写入日志信息：日志文件名+分隔符+日志内容
        ByteBuf buf = ctx.alloc().buffer(file.length + msg.length + 1);
        buf.writeBytes(file);
        buf.writeByte(LogEvent.SEPARATOR);
        buf.writeBytes(msg);

        // 编码为DatagramPacket，然后添加到出站消息列表中
        out.add(new DatagramPacket(buf,remoteAddress));
    }
}
