package com.fandou.learning.netty.action.chapter10;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * 整数转为字符串：继承MessageToMessageDecoder类
 * Integer表示传入的数据为Integer类型，意味着channelpipeline中，前面应该有字节解码为Integer的解码器再传递到当前解码器进行二次解码
 */
public class IntegerToStringDecoder extends MessageToMessageDecoder<Integer> {
    @Override
    protected void decode(ChannelHandlerContext ctx, Integer msg, List out) throws Exception {
        // 将Integer转码为String
        out.add(String.valueOf(msg));
    }
}
