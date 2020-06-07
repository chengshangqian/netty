package com.fandou.learning.netty.core.chapter13.client.handler;

import com.fandou.learning.netty.core.chapter13.client.ChatClient;
import com.fandou.learning.netty.core.chapter13.protocol.IMMessage;
import com.fandou.learning.netty.core.chapter13.protocol.IMProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 控制台终端聊天室处理器
 * 实现Runnable接口，新启动一个线程开启聊天会话
 */
public class ChatClientHandler extends SimpleChannelInboundHandler<IMMessage> implements Runnable {

    /**
     * logback日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChatClientHandler.class);

    /**
     * 聊天会话线程
     */
    private Thread session = new Thread(this);

    /**
     * 上下文
     */
    private ChannelHandlerContext ctx;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 指定用户昵称初始化控制台终端聊天室处理器
     *
     * @param nickname 用户昵称
     */
    public ChatClientHandler(String nickname){
        this.nickname = nickname;
    }

    /**
     * 发送聊天内容
     *
     * @param imMessage 聊天内容
     * @return
     */
    private boolean sendMessage(IMMessage imMessage) {
        // 发送消息操作类型
        String cmd = imMessage.getCmd();

        // 发送消息
        ctx.channel().writeAndFlush(imMessage);

        // 每次发送消息后，检查发送的消息是否是登出操作
        return !IMProtocol.isLogout(cmd);
    }

    /**
     * 通道激活时，保存通道处理器上下文，然后登录聊天室服务器
     *
     * @param ctx 通道处理器上下文
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 保存通道处理器上下文
        this.ctx = ctx;

        // 发送登录请求
        IMMessage imMessage = new IMMessage(IMProtocol.LOGIN.getName(), ChatClient.TERMINAL,System.currentTimeMillis(),this.nickname);
        sendMessage(imMessage);

        logger.info("成功连接服务器，已执行登录动作...");

        // 启动终端输入聊天内容
        session.start();
    }

    /**
     * 处理收到的聊天内容
     *
     * @param ctx 通道处理器上下文
     * @param msg 需要处理的消息
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMMessage msg) throws Exception {
        // 聊天内容
        String content = "";

        // 如果消息不为空
        if(null != msg){
            // 格式化为 发送者:消息内容
            content = msg.getSender() + ":" + removeHtmlTag(msg.getContent());
        }

        // 打印聊天消息
        logger.info(content);
    }

    /**
     * 移除一些非原始内容中的标签
     *
     * @param content 聊天内容
     * @return
     */
    private String removeHtmlTag(String content) {
        String scriptRegex = "<script[^>]*?>[\\s\\S]*?<\\/script>";
        String styleRegex = "<style[^>]*?>[\\s\\S]*?<\\/style>";
        String htmlRegex = "<[^>]+>";

        Pattern scriptPattern = Pattern.compile(scriptRegex,Pattern.CASE_INSENSITIVE);
        Matcher scriptMatcher = scriptPattern.matcher(content);
        content = scriptMatcher.replaceAll("");

        Pattern stylePattern = Pattern.compile(styleRegex,Pattern.CASE_INSENSITIVE);
        Matcher styleMatcher = stylePattern.matcher(content);
        content = styleMatcher.replaceAll("");

        Pattern htmlPattern = Pattern.compile(htmlRegex,Pattern.CASE_INSENSITIVE);
        Matcher htmlMatcher = htmlPattern.matcher(content);
        content = htmlMatcher.replaceAll("");

        return content.trim();
    }

    /**
     * 处理捕获异常
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("与服务器断开 -> ");
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 启动新线程开启会话
     */
    @Override
    public void run() {
        // 在一个新的线程启动终端，开始聊天会话
        startSession();
    }

    /**
     * 启动终端输入聊天内容
     */
    private void startSession(){
        logger.info(nickname + ",你好，请在控制台输入对话内容");
        IMMessage imMessage = null;
        Scanner scanner = new Scanner(System.in);
        do {
            if(scanner.hasNext()){
                String input = scanner.nextLine();
                if("exit".equals(input) || "quit".equals(input)){
                    imMessage = new IMMessage(IMProtocol.LOGOUT.getName(),ChatClient.TERMINAL,System.currentTimeMillis(),nickname);
                }
                else{
                    imMessage = new IMMessage(IMProtocol.CHAT.getName(),System.currentTimeMillis(),nickname,input);
                }
            }
        }
        while (sendMessage(imMessage));

        scanner.close();
        closeSession();
    }

    /**
     * 关闭聊天会话
     */
    private void closeSession(){
        try {
            session.join(500);
            ctx.channel().close();
        } catch (InterruptedException e) {
            logger.info("聊天线程{}退出失败...",null != session ? session.getName() : "");
        }
    }
}
