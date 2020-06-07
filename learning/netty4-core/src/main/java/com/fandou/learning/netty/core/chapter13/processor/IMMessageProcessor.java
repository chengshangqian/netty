package com.fandou.learning.netty.core.chapter13.processor;

import com.alibaba.fastjson.JSONObject;
import com.fandou.learning.netty.core.chapter13.client.ChatClient;
import com.fandou.learning.netty.core.chapter13.protocol.IMDecoder;
import com.fandou.learning.netty.core.chapter13.protocol.IMEncoder;
import com.fandou.learning.netty.core.chapter13.protocol.IMMessage;
import com.fandou.learning.netty.core.chapter13.protocol.IMProtocol;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 即时通讯消息处理器
 */
public class IMMessageProcessor {

    /**
     * 在线用户
     */
    private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 通道属性键
     */
    public static final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
    public static final AttributeKey<String> ATTRS_NICK_NAME = AttributeKey.valueOf("nickName");
    public static final AttributeKey<String> ATTRS_REMOTE_ADDRESS = AttributeKey.valueOf("remoteAddress");
    public static final AttributeKey<String> ATTRS_FROM = AttributeKey.valueOf("from");

    /**
     * 即时消息编解码器
     */
    private IMEncoder encoder = new IMEncoder();
    private IMDecoder decoder = new IMDecoder();

    /**
     * 获取用户昵称
     *
     * @param client
     * @return
     */
    public String getNickName(Channel client){
        return client.attr(ATTRS_NICK_NAME).get();
    }

    /**
     * 获取客户端IP地址
     *
     * @param client
     * @return
     */
    public String getAddress(Channel client) {
        return client.remoteAddress().toString().replaceFirst("/","");
    }

    /**
     * 获取通道属性
     *
     * @param client
     * @return
     */
    public JSONObject getAttrs(Channel client){
        try{
            return client.attr(ATTRS).get();
        } catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * 设置通道属性
     *
     * @param client
     * @param key
     * @param value
     */
    public void setAttrs(Channel client,String key,Object value){
        try{
            JSONObject json = client.attr(ATTRS).get();
            json.put(key,value);
            client.attr(ATTRS).set(json);
        } catch (Exception ex){
            JSONObject json = new JSONObject();
            json.put(key,value);
            client.attr(ATTRS).set(json);
        }
    }

    /**
     * 获取系统当前时间
     *
     * @return
     */
    private long sysTime() {
        return System.currentTimeMillis();
    }

    /**
     * 响应处理收到来自客户端的消息：
     * 将消息格式化后，广播给其它所有聊天室中的用户。
     * 客户端可以发送的消息指令为登录|聊天|送花|登出，只需要处理这四种即可。系统消息由负端主动发送。
     *
     * @param client 消息来源客户端，可能来自websocket端，也可能是来自终端聊天室（控制台）
     * @param imMessage 消息
     */
    public void sendMessage(Channel client, IMMessage imMessage) {
        // 消息不能为空
        if(null == imMessage){
            return;
        }

        // 消息指令
        String cmd = imMessage.getCmd();

        // 处理登录消息
        if(IMProtocol.isLogin(cmd)){
            login(client,imMessage);
        }

        // 处理聊天消息
        else if(IMProtocol.isChat(cmd)){
            chat(client,imMessage);
        }

        // 处理送花消息
        else if(IMProtocol.isFlower(cmd)){
            flower(client,imMessage);
        }

        // 处理登出消息
        else if(IMProtocol.isLogout(cmd)){
            logout(client);
        }
    }

    /**
     * 响应处理收到来自客户端的消息
     *
     * @param client 消息来源客户端，主要是websocket端
     * @param message 消息
     */
    public void sendMessage(Channel client, String message) {
        sendMessage(client,decoder.decode(message));
    }

    /**
     * 登录
     *
     * @param client
     * @param imMessage
     */
    private void login(Channel client,IMMessage imMessage){
        client.attr(ATTRS_NICK_NAME).getAndSet(imMessage.getSender());
        client.attr(ATTRS_REMOTE_ADDRESS).getAndSet(getAddress(client));
        client.attr(ATTRS_FROM).getAndSet(imMessage.getTerminal());
        onlineUsers.add(client);

        // 系统消息
        String system = IMProtocol.SYSTEM.getName();

        // 发送时间
        long sendingTime = sysTime();

        // 当前在线人数
        int online = onlineUsers.size();

        // 消息内容
        String content = getNickName(client) + "加入了聊天室...";

        // 创建系统消息，通知所有人有新用户加入聊天室
        imMessage = new IMMessage(system,sendingTime,online,content);
        imMessage.setSender("admin");

        for (Channel channel : onlineUsers) {
            // 如果是新加入的用户本身，通知内容稍微该表
            if(channel == client){
                imMessage.setContent("已与服务器建立连接...");
            }
            else{
                imMessage.setContent(content);
            }

            // 如果是控制台终端客户端用户，直接发送即时消息对象
            if(ChatClient.TERMINAL.equals(channel.attr(ATTRS_FROM).get())){
                channel.writeAndFlush(imMessage);
                continue;
            }

            // 如果是WebSocket端用户，发送格式化的字符串消息
            String message = encoder.encode(imMessage);
            channel.writeAndFlush(new TextWebSocketFrame(message));
        }
    }

    /**
     * 发送聊天信息
     *
     * @param client
     * @param imMessage
     */
    private void chat(Channel client,IMMessage imMessage){
        for (Channel channel : onlineUsers) {
            boolean isSelf = (channel == client);
            if(isSelf){
                imMessage.setSender("you");
            }
            else{
                imMessage.setSender(getNickName(client));
            }
            imMessage.setSendingTime(sysTime());

            // 控制台终端用户
            if(ChatClient.TERMINAL.equals(channel.attr(ATTRS_FROM).get()) && !isSelf){
                channel.writeAndFlush(imMessage);
                continue;
            }

            // WebSocket端用户
            String content = encoder.encode(imMessage);
            channel.writeAndFlush(new TextWebSocketFrame(content));
        }
    }

    /**
     * 送花
     *
     * @param client
     * @param imMessage
     */
    private void flower(Channel client, IMMessage imMessage){
        JSONObject attrs = getAttrs(client);
        long currTime = sysTime();

        // 送花频率60秒
        if(null != attrs){
            long lastTime = attrs.getLongValue("lastFlowerTime");
            int seconds = 60;
            long sub = currTime - lastTime;
            if(sub < 1000 * seconds){
                imMessage.setSender("you");
                imMessage.setCmd(IMProtocol.SYSTEM.getName());
                imMessage.setContent("您送鲜花太频繁," + (seconds - Math.round(sub/1000)) + "秒后再试.");

                // 鲜花和表情支持WebSocket端用户，所以发送格式化的字符串消息
                String content = encoder.encode(imMessage);
                client.writeAndFlush(content);
                return;
            }
        }

        // 正常送花
        for (Channel channel : onlineUsers) {
            boolean isSelf = (channel == client);
            if(isSelf){
                imMessage.setSender("you");
                imMessage.setContent("您给大家送了一波鲜花雨...");
                setAttrs(client,"lastFlowerTime",currTime);
            }
            else{
                imMessage.setSender(getNickName(client));
                imMessage.setContent(getNickName(client) + "送来了一波鲜花雨...");
            }
            imMessage.setSendingTime(sysTime());

            // 控制台终端用户
            if(ChatClient.TERMINAL.equals(channel.attr(ATTRS_FROM).get()) && !isSelf){
                channel.writeAndFlush(imMessage);
                continue;
            }

            // WebSocket端用户
            String content = encoder.encode(imMessage);
            channel.writeAndFlush(new TextWebSocketFrame(content));
        }
    }

    /**
     * 登出
     *
     * @param client
     */
    private void logout(Channel client){
        String nickName = getNickName(client);
        if(null == nickName){
            return;
        }

        // 用户退出登录
        onlineUsers.remove(client);

        // 系统消息
        String system = IMProtocol.SYSTEM.getName();

        // 发送时间
        long sendingTime = sysTime();

        // 当前在线人数
        int online = onlineUsers.size();

        // 消息内容
        String content = nickName + "离开了聊天室...";

        // 构造即时消息对象
        IMMessage imMessage = new IMMessage(system,sendingTime,online,content);
        imMessage.setSender("admin");

        // 通知其它用户，当前用户已经离开聊天室
        for (Channel channel : onlineUsers) {
            // 如果是控制台终端客户端用户，直接发送即时消息对象
            if(ChatClient.TERMINAL.equals(channel.attr(ATTRS_FROM).get())){
                channel.writeAndFlush(imMessage);
                continue;
            }

            // 如果是WebSocket端用户，发送格式化的字符串消息
            String message = encoder.encode(imMessage);
            channel.writeAndFlush(new TextWebSocketFrame(message));
        }
    }
}
