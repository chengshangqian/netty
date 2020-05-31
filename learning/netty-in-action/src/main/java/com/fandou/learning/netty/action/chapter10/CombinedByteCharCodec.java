package com.fandou.learning.netty.action.chapter10;

import io.netty.channel.CombinedChannelDuplexHandler;

/**
 * 字节-字符编解码器
 */
public class CombinedByteCharCodec extends CombinedChannelDuplexHandler<ByteToCharDecoder,CharToByteEncoder> {
    public CombinedByteCharCodec(){
        super(new ByteToCharDecoder(),new CharToByteEncoder());
    }
}
