package com.fandou.learning.netty.action.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试出站的绝对值数据:测试出站信息
 */
class AbsIntegerEncoderTest {

    @Test
    void testAbsIntegerEncoded(){
        // 创建原始数据
        ByteBuf source = Unpooled.buffer();
        for (int i = 1; i < 10; i++) {
            // 每个负整数4个字节
            source.writeInt(i * -1);
        }

        // 派生一个源数据的拷贝，然后写入到channel中
        ByteBuf output = source.copy();
        EmbeddedChannel channel = new EmbeddedChannel( new AbsIntegerEncoder());

        // 断言将数据写入channel,模拟数据出站：如果可以使用readOutbound从channel中读取到数据，则返回true
        assertTrue(channel.writeOutbound(output));
        // 将channel标记为已完成状态：写入完成/结束写入
        assertTrue(channel.finish());

        // 读取出站的数据，相当于远程主机读取其入站数据
        for (int i = 0; i < 9; i++) {
            int sourceValue = source.readInt();
            int value = (Integer)channel.readOutbound();

            // 断言原始值和编码后的指不相等
            assertNotEquals(sourceValue,value);
            // 断言原始值的绝对值和编码后的值相等
            assertEquals(Math.abs(sourceValue),value);

            // 如果上面的断言都正确的，则将看到下面打印的信息
            System.out.println(" 原始value => " + sourceValue);
            System.out.println(" 编码后出站的value => " + value);
        }

        // 断言channel中的数据已经读取完毕/空
        assertNull(channel.readOutbound());
    }
}