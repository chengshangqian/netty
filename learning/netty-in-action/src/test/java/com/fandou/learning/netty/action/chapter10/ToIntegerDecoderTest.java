package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToIntegerDecoderTest {

    @Test
    void testToIntegerDecoder(){
        ByteBuf input = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            input.writeInt(i);
        }

        // 额外多写入一个字节
        input.writeByte(9);

        // 一共37个字节
        assertEquals(37,input.readableBytes());

        // 创建测试用的channel
        EmbeddedChannel channel = new EmbeddedChannel(new ToIntegerDecoder());

        // 向channel写入测试数据
        assertTrue(channel.writeInbound(input));
        assertTrue(channel.finish());

        // 从channel中读取入站数据
        for (int i = 0; i < 10; i++) {
            Integer value = (Integer)channel.readInbound();
            System.out.println("i = " + value);

            // 断言入站的数据
            if(i == 9){
                // 最后写入input的数据9将不会被读取到
                assertNull(value);
                assertNotEquals(i,value);
            }
            else {
                assertEquals(i,value);
            }
        }
    }
}