package com.fandou.learning.netty.core.chapter13.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * 即时消息编码器
 */
public class IMEncoder extends MessageToByteEncoder<IMMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IMMessage msg, ByteBuf out) throws Exception {
        // 使用MessagePack将IMMessage消息编码为字节数组
        out.writeBytes(new MessagePack().write(msg));
    }

    /**
     * 将IMMessage编码为格式化的字符串
     *
     * @param imMessage
     * @return
     */
    public String encode(IMMessage imMessage){
        if(null == imMessage){
            return "";
        }

        // 发送消息的指令或类型
        String cmd = imMessage.getCmd();

        // 消息最基本组成：指令+发送时间
        String message = "[" + cmd + "]" + "[" + imMessage.getSendingTime() + "]" ;

        // 如果是登录|聊天|送花，需要消息发送者
        if(IMProtocol.isLogin(cmd) || IMProtocol.isChat(cmd) || IMProtocol.isFlower(cmd)){
            message += "[" + imMessage.getSender() + "]";

            // 如果是送花，额外添加terminal信息
            if(IMProtocol.isFlower(cmd)){
                message += "[" + imMessage.getTerminal() + "]";
            }
        }

        // 如果是系统消息，附加消息在线人数
        else if(IMProtocol.isSystem(cmd)){
            message += "[" + imMessage.getOnline() + "]";
        }

        // 如果消息内容不为空，最后加上消息内容
        boolean invalidContent = null == imMessage.getContent() || "".equals(imMessage.getContent().trim());
        if(!invalidContent){
            message += " - " + imMessage.getContent();
        }

        return message;
    }
}
