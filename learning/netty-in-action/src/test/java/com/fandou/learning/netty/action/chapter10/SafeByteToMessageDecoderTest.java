package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeByteToMessageDecoderTest {

    @Test
    void testSafeByteToMessageDecoder(){
        // 创建一个256字节长度的字符串的ByteBuf
        ByteBuf normalBtyes = Unpooled.buffer();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            s.append("0");
        }
        System.out.println("normalBtyes => " +  s.toString().getBytes(CharsetUtil.UTF_8).length);
        normalBtyes.writeBytes(s.toString().getBytes(CharsetUtil.UTF_8));

        // 创建一个1024字节长度的字符串的ByteBuf
        ByteBuf maxSizeBtyes = Unpooled.buffer();
        s = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            s.append("0");
        }
        System.out.println("maxSizeBtyes => " +  s.toString().getBytes(CharsetUtil.UTF_8).length);
        maxSizeBtyes.writeBytes(s.toString().getBytes(CharsetUtil.UTF_8));

        // 创建一个1025字节长度的字符串的ByteBuf
        ByteBuf tooLongBtyes = Unpooled.buffer();
        s = new StringBuilder();
        for (int i = 0; i < 1025; i++) {
            s.append("0");
        }
        System.out.println("tooLongBtyes => " +  s.toString().getBytes(CharsetUtil.UTF_8).length);
        tooLongBtyes.writeBytes(s.toString().getBytes(CharsetUtil.UTF_8));

        // 创建测试用的channel
        EmbeddedChannel channel = new EmbeddedChannel(new SafeByteToMessageDecoder());

        // 断言小于1024字节长度的字符串可以正常写入channel中
        assertTrue(channel.writeInbound(normalBtyes));

        // 断言等于1024字节长度的字符串可以正常写入channel中
        assertTrue(channel.writeInbound(maxSizeBtyes));

        try {
            // 尝试写入大于1024字节长度的字符串到channel中，预期将抛出并捕获TooLongFrameException异常
            channel.writeInbound(tooLongBtyes);

            // 如果没有抛出异常，断言测试失败
            fail();
        } catch (TooLongFrameException ex){
            // 打印捕获的异常栈信息
            ex.printStackTrace();
        }

        // 标识channel状态为完成
        assertTrue(channel.finish());

        // 从channel中读取字符串内容
        String n = channel.readInbound();
        // 断言读取的字符串内容为256个字节长度
        assertEquals(256,n.getBytes(CharsetUtil.UTF_8).length);

        // 从channel中读取字符串内容
        String m = channel.readInbound();
        // 断言读取的字符串内容为1024个字节长度
        assertEquals(1024,m.getBytes(CharsetUtil.UTF_8).length);

        // 从channel中读取字符串内容
        String t = channel.readInbound();
        // 由于第3次写入的内容1025超出指定的最大长度1024字节，写入失败，所以断言channel中第3次读取的内容应为null
        assertNull(t);
    }
}