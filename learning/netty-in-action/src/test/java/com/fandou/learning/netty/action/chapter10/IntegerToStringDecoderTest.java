package com.fandou.learning.netty.action.chapter10;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegerToStringDecoderTest {

    @Test
    void testIntegerToString(){
        ByteBuf input = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            input.writeInt(i);
        }

        EmbeddedChannel channel = new EmbeddedChannel(new ToIntegerDecoder(),new IntegerToStringDecoder());

        channel.writeInbound(input);
        channel.finish();

        for (int i = 0; i < 9; i++) {
            String value = (String) channel.readInbound();
            System.out.println("value = " + value);
            assertEquals(value,(""+i));
        }

    }
}