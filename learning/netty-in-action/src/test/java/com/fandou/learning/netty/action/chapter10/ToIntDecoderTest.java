package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToIntDecoderTest {

    @Test
    void testToIntDecoder(){
        // 创建测试数据
        ByteBuf input = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            input.writeInt(i);
        }

        // 额外写入一个字节
        input.writeByte(9);

        // 创建测试用的channel，并将数据写入channel中
        EmbeddedChannel channel = new EmbeddedChannel(new ToIntDecoder());
        assertTrue(channel.writeInbound(input));
        assertTrue(channel.finish());

        // 从channel中读取入站的数据
        for (int i = 0; i < 10 ; i++) {
            // 数字9将不会被读取，并且抛出异常：实际测试没有抛出异常...
            Integer value = (Integer)channel.readInbound();
            System.out.println(" i = " + value);
            if(i == 9) {
                assertNull(value);
                assertNotEquals(i,value);
            } else {
                // 0-8
                assertEquals(i, value);
            }
        }

    }
}