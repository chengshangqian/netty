package com.fandou.learning.netty.action.chapter13;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * DatagramPacket-LogEvent解码器
 */
public class LogEventDecoder extends MessageToMessageDecoder<DatagramPacket> {
    /**
     * 将DatagramPacket解码为LogEvent
     *
     * @param ctx
     * @param datagramPacket
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket datagramPacket, List<Object> out) throws Exception {
        // 读取缓冲的数据包(字节数据)：其结构为 日志文件名 + 分割符（占1个字节） + 日志内容
        ByteBuf data = datagramPacket.content();

        // 可读取的数据包总长度
        int readable = data.readableBytes();

        // 分隔符索引位置
        int separatorIndex = data.indexOf(0,readable,LogEvent.SEPARATOR);

        // 日志文件名占数据包的长度：刚好是分割符的索引大小
        int logfileLength = separatorIndex;
        // 日志文件名从索引0开始
        ByteBuf logfileBuf = data.slice(0,logfileLength);
        // 读取日志文件名
        final String logfile = logfileBuf.toString(CharsetUtil.UTF_8);

        // 消息即日志内容（一行）的长度：为数据包总长度 - 日志文件名长度 - 分隔符长度(分割符占1个字节长度)
        int msgLength = readable - logfileLength - 1;
        // 日志内容索引从分隔符后开始,即日志文件名长度 + 1（分割符所占字节长度）
        ByteBuf msgBuf = data.slice(logfileLength + 1,msgLength);
        // 读取日志文件名
        final String msg = msgBuf.toString(CharsetUtil.UTF_8);

        // 日志发送来源/主机
        InetSocketAddress source = datagramPacket.sender();
        // 本地接收到日志的时间
        long received = System.currentTimeMillis();

        // 编码为LogEvent
        LogEvent logEvent = new LogEvent(source,received,logfile,msg);

        // 添加到消息列表中
        out.add(logEvent);
    }
}
