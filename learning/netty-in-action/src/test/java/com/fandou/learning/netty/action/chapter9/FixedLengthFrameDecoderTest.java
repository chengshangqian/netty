package com.fandou.learning.netty.action.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试自定义的定长帧解码器：测试入站消息
 */
class FixedLengthFrameDecoderTest {

    /**
     * 测试入站消息
     */
    @Test
    void testFramesDecoded() {
        // 创建一个ByteBuf，并存储9个字节
        ByteBuf source = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            source.writeByte(i);
        }

        // 派生一个新的ByteBuf实例in，它具有自己的读索引和写索引以及标记索引，
        // 但其内容和源ByteBuf则是共享的，即修改了in，同时也会修改source
        ByteBuf in = source.duplicate();

        // 定义解码帧的长度
        int frameLength = 3;

        // 创建将要被测试的定长帧解码器
        FixedLengthFrameDecoder fixedLengthFrameDecoder = new FixedLengthFrameDecoder(frameLength);

        // 创建将用于测试自定义处理器的EmbeddedChannel管道
        EmbeddedChannel channel = new EmbeddedChannel(fixedLengthFrameDecoder);

        // 断言writeInbound方法调用，模拟远程主机发送数据：将入站消息input写入到channel中
        // 如果可以通过readInbound方法从channel中读取数据，则返回true
        assertTrue(channel.writeInbound(in.retain()));

        // 断言finish方法调用：标记channel为已完成状态，即写入结束
        // 如果channel中有可被读取的（出站或入站）数据，则返回出，finish方法内部会调用close方法关闭channel和释放资源
        assertTrue(channel.finish());

        /**** 读取入站的信息 ****/
        // 读取所有生成的消息：每次读取一个数据包（帧），即3个字节
        ByteBuf message = (ByteBuf) channel.readInbound();
        // 源数据source中的前3个字节
        ByteBuf firstThree = source.readSlice(frameLength);
        // 断言source中的首3个字节与接收到的首个数据包(帧,3个字节)message中的内容是否一致
        assertEquals(firstThree,message);
        // 分别打印内容查看
        System.out.println("源数据中的【前面3个】字节 => " + ByteBufUtil.hexDump(firstThree));
        System.out.println("接受到的【第1帧】消息 => " + ByteBufUtil.hexDump(message));
        // 释放清空message,后面继续读取保存下一个帧
        message.release();

        // 从channel入站数据中读取第2帧数据
        message = (ByteBuf)channel.readInbound();
        // 源数据source中的中间3个字节
        // ByteBuf的API总read*的方法，都会移动当前的读索引下标，所以这里再次调用，读取的时第2个[3字节数据]，而非首个3字节数据
        ByteBuf secondThree = source.readSlice(frameLength);
        assertEquals(secondThree,message);
        // 分别打印内容查看
        System.out.println("源数据中的【中间3个】字节 => " + ByteBufUtil.hexDump(secondThree));
        System.out.println("接受到的【第2帧】消息 => " + ByteBufUtil.hexDump(message));
        message.release();

        message = (ByteBuf)channel.readInbound();
        ByteBuf thirdThree = source.readSlice(frameLength);
        assertEquals(thirdThree,message);
        System.out.println("源数据中的【最后3个】字节 => " + ByteBufUtil.hexDump(thirdThree));
        System.out.println("接受到的【第3帧】消息 => " + ByteBufUtil.hexDump(message));
        message.release();

        // 断言channel中已经无数据
        // 此时所有入站数据已经读取完毕，尝试从channel中读取入站数据，将返回null
        assertNull(channel.readInbound());
        // 释放资源
        source.release();
    }

    @Test
    void testFrameDecoded2() {
        System.out.println("testFrameDecoded2测试方法...");
        ByteBuf source = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            source.writeByte(i);
        }

        ByteBuf in = source.duplicate();

        int frameLength = 3;
        EmbeddedChannel channel = new EmbeddedChannel(new FixedLengthFrameDecoder(frameLength));

        // 此处先写入2个字节
        // 按照解码器的规则，数据帧中的字节长度不符合解码器的长度，readInbound将不会去读取到数据，所以此时调用writeInbound写入2字节数据将返回false
        assertFalse(channel.writeInbound(in.readBytes(2)));

        // 将剩下的77字节数据写入到channel中
        // 此时由于channel合计被写入了9字节数据，刚好是3的整数倍，程序有数据可以读取，所以writeInbound方法返回true
        assertTrue(channel.writeInbound(in.readBytes(7)));

        // 断言finish方法调用：标记channel为已完成状态，即写入结束
        // 如果channel中有可被读取的（出站或入站）数据，则返回出，finish方法内部会调用close方法关闭channel和释放资源
        assertTrue(channel.finish());

        /**** 测试消息的读取操作 ****/
        // 读取所有生成的消息：每次读取一个数据包（帧），即3个字节
        ByteBuf message = (ByteBuf) channel.readInbound();
        // 源数据source中的前3个字节
        ByteBuf firstThree = source.readSlice(frameLength);
        // 断言source中的首3个字节与接收到的首个数据包(帧,3个字节)message中的内容是否一致
        assertEquals(firstThree,message);
        // 分别打印内容查看
        System.out.println("源数据中的【前面3个】字节 => " + ByteBufUtil.hexDump(firstThree));
        System.out.println("接受到的【第1帧】消息 => " + ByteBufUtil.hexDump(message));
        // 释放清空message,后面继续读取保存下一个帧
        message.release();

        // 从channel入站数据中读取第2帧数据
        message = (ByteBuf)channel.readInbound();
        // 源数据source中的中间3个字节
        // ByteBuf的API总read*的方法，都会移动当前的读索引下标，所以这里再次调用，读取的时第2个[3字节数据]，而非首个3字节数据
        ByteBuf secondThree = source.readSlice(frameLength);
        assertEquals(secondThree,message);
        // 分别打印内容查看
        System.out.println("源数据中的【中间3个】字节 => " + ByteBufUtil.hexDump(secondThree));
        System.out.println("接受到的【第2帧】消息 => " + ByteBufUtil.hexDump(message));
        message.release();

        message = (ByteBuf)channel.readInbound();
        ByteBuf thirdThree = source.readSlice(frameLength);
        assertEquals(thirdThree,message);
        System.out.println("源数据中的【最后3个】字节 => " + ByteBufUtil.hexDump(thirdThree));
        System.out.println("接受到的【第3帧】消息 => " + ByteBufUtil.hexDump(message));
        message.release();

        // 断言channel中已经无数据
        // 此时所有入站数据已经读取完毕，尝试从channel中读取入站数据，将返回null
        assertNull(channel.readInbound());
        // 释放资源
        source.release();
    }

}