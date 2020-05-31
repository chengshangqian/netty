package com.fandou.learning.netty.action.chapter10;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegerToStringEncoderTest {
    @Test
    void testIntegerToStringEncoder(){
        // 创建测试channel
        EmbeddedChannel channel = new EmbeddedChannel(new IntegerToStringEncoder());

        // 断言整型数据写入channel成功完成
        for (int i = 0; i < 3; i++) {
            assertTrue(channel.writeOutbound(i));
        }
        // 非Integer类型可以出站，不过不会被传递到IntegerToStringEncoder编码器进行编码，encode方法不会打印这个消息
        assertTrue(channel.writeOutbound("字符串类型数据"));
        assertTrue(channel.finish());

        for (int i = 0; i < 3; i++) {
            // 尝试从channel中读取出站数据，断言相等字符串内容
            assertEquals("" + i,channel.readOutbound());
        }

        // 打印字符串
        System.out.println("test ===> " + channel.readOutbound());
    }
}