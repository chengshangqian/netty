package com.fandou.learning.netty.core.chapter13.protocol;

import lombok.Data;
import org.msgpack.annotation.Message;

/**
 * 自定义协议消息
 */
@Data
@Message
public class IMMessage {

    // 协议指令
    private String cmd;

    // 命令发送时间
    private long sendingTime;

    // 当前在线人数
    private int online;

    // 发送人
    private String sender;

    // 接收人
    private String receiver;

    //消息内容
    private String content;

    // 终端
    private String terminal;

    // ip地址和端口
    private String remoteAddress;

    public IMMessage(){

    }

    public IMMessage(String cmd, long sendingTime, int online, String content){
        this.cmd = cmd;
        this.sendingTime = sendingTime;
        this.online = online;
        this.content = content;
    }

    public IMMessage(String cmd, String terminal, long sendingTime, String sender){
        this.cmd = cmd;
        this.sendingTime = sendingTime;
        this.sender = sender;
        this.terminal = terminal;
    }

    public IMMessage(String cmd, long sendingTime, String sender, String content){
        this.cmd = cmd;
        this.sendingTime = sendingTime;
        this.sender = sender;
        this.content = content;
    }

    @Override
    public String toString() {
        return "IMMessage{" +
                "cmd='" + cmd + '\'' +
                ", sendingTime=" + sendingTime +
                ", online=" + online +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", terminal='" + terminal + '\'' +
                '}';
    }
}
