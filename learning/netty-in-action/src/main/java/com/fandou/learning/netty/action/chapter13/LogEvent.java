package com.fandou.learning.netty.action.chapter13;

import java.net.InetSocketAddress;

/**
 * 定义日志事件
 */
public class LogEvent {
    /**
     * 日志文件名和日志内容（行）的分割符
     */
    public static final byte SEPARATOR = (byte)';';

    /**
     * 日志文件名：完整路径，源主机程序读取日志文件路径时获取并设置
     */
    private final String logfile;

    /**
     * 日志内容：源主机程序从日志文件中读取
     */
    private final String msg;


    /**
     * 发送日志消息的本地源主机地址：远程主机端程序解码时设置
     */
    private final InetSocketAddress source;

    /**
     * 收到日志的时间：远程主机端程序解码时设置
     */
    private final long received;

    /**
     * 初始化日志事件：适合源主机发送日志时使用
     *
     * @param logfile
     * @param msg
     */
    public LogEvent(String logfile, String msg) {
        this(null,-1,logfile,msg);
    }

    /**
     * 初始化日志事件：远程主机接收日志解码时使用
     *
     * @param source
     * @param received
     * @param logfile
     * @param msg
     */
    public LogEvent(InetSocketAddress source, long received, String logfile, String msg) {
        this.source = source;
        this.logfile = logfile;
        this.msg = msg;
        this.received = received;
    }

    public InetSocketAddress getSource() {
        return source;
    }

    public String getLogfile() {
        return logfile;
    }

    public String getMsg() {
        return msg;
    }

    public long getReceived() {
        return received;
    }
}
