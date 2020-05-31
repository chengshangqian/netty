package com.fandou.learning.netty.action.chapter10;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * 整型-字符串编码器：指定泛型为Integer类型，其它类型的数据不会被传入进来
 */
public class IntegerToStringEncoder extends MessageToMessageEncoder<Integer> {
    /**
     * 将整数转换为字符串出站
     *
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Integer msg, List<Object> out) throws Exception {
        // 将Integer类型的消息数据转换为字符串，添加到出站消息列表中
        System.out.println(" msg => " + msg);
        out.add(String.valueOf(msg));
    }
}
