package com.fandou.learning.netty.core.chapter13.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 即时消息解码器
 */
public class IMDecoder extends ByteToMessageDecoder {

    /**
     * 即时消息格式正则模式
     */
    private Pattern pattern = Pattern.compile("^\\[(.*)\\](\\s\\-\\s(.*))?");

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            final int length = in.readableBytes();
            final byte[] array = new byte[length];
            String content = new String(array, in.readerIndex(), length);

            // 接收到的消息不为空时，进行验证
            boolean invalidContent = null == content || "".equals(content.trim());
            if (!invalidContent) {
                // 如果不是协议指令开头，将处理器从当前pipeline中移除?
                if (!IMProtocol.startWithIMProtocol(content.trim())) {
                    ctx.channel().pipeline().remove(this);
                    return;
                }
            }

            // 读取到字节数组中，使用MessagePack将字节数组内容解析为IMMessage类型的对象
            in.getBytes(in.readerIndex(), array, 0, length);
            out.add(new MessagePack().read(array, IMMessage.class));
            in.clear();
        } catch(MessageTypeException ex){
            ctx.channel().pipeline().remove(this);
        }
    }

    /**
     * 将一条原始的字符串指令消息解码为IMMessage对象实例
     *
     * @param msg 原始字符串指令消息：登录|聊天|送花
     * @return
     */
    public IMMessage decode(String msg){
        if (null == msg || "".equals(msg.trim())) {
            return null;
        }

        try {
            Matcher m = pattern.matcher(msg);
            String header = "";
            String content = "";

            if(m.matches()){
                header = m.group(1);
                content = m.group(3);
            }

            String[] headers = header.split("\\]\\[");
            long time = 0;
            try{
                time = Long.parseLong(headers[1]);
            } catch (Exception e){
                e.printStackTrace();
            }

            String nickname = headers[2];
            nickname = nickname.length() < 10 ? nickname : nickname.substring(0,9);

            if(msg.startsWith("[" + IMProtocol.LOGIN.getName() + "]")){
                return new IMMessage(headers[0],headers[3],time,nickname);
            }

            else if(msg.startsWith("[" + IMProtocol.CHAT.getName() + "]")){
                return new IMMessage(headers[0],time,nickname,content);
            }

            else if(msg.startsWith("[" + IMProtocol.FLOWER.getName() + "]")){
                return new IMMessage(headers[0],headers[3],time,nickname);
            }

            else if(msg.startsWith("[" + IMProtocol.LOGOUT.getName() + "]")){
                return new IMMessage(headers[0],headers[3],time,nickname);
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
