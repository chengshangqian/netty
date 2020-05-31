package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortToByteEncoderTest {
    @Test
    void testShortToByteEncoder(){
        // 创建测试管道channel
        EmbeddedChannel channel = new EmbeddedChannel(new ShortToByteEncoder());

        // 断言写出数据到channel中成功完成
        for (int i = 0; i < 3; i++) {
            assertTrue(channel.writeOutbound(Short.valueOf("" + i)));
        }
        // 非Short类型可以出站，不过不会被传递到ShortToByteEncoder编码器进行编码，encode方法不会打印这个消息
        assertTrue(channel.writeOutbound("字符串类型数据"));
        assertTrue(channel.finish());

        // 尝试从channel中读取出站数据
        for (int i = 0; i < 3; i++) {
            // 断言出站的ByteBuf数据内容为Short类型的数据
            assertEquals(Short.valueOf("" + i),((ByteBuf)channel.readOutbound()).readShort());
            //assertEquals(Short.valueOf("" + i),channel.readOutbound());
        }

        // 打印字符串
        System.out.println("test ===> " + channel.readOutbound());
    }
}