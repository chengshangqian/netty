package com.fandou.learning.netty.core.chapter4.bio;

import com.fandou.learning.netty.core.chapter4.server.Response;
import io.netty.util.CharsetUtil;

import java.io.OutputStream;

/**
 * HTTP响应
 */
public class BIOHttpResponse implements Response {

    // 响应客户端的输出流
    private OutputStream serverOutputStream;

    /**
     * 初始化HTTP响应
     *
     * @param serverOutputStream 响应客户端的输出流
     */
    public BIOHttpResponse(OutputStream serverOutputStream){
        this.serverOutputStream = serverOutputStream;
    }

    /**
     * 发送响应给客户端
     *
     * @param message 发送给客户端的消息内容
     * @throws Exception
     */
    @Override
    public void write(String message) throws Exception{

        // 构造HTTP响应内容
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n")
                .append("Content-Type:text/html;charset=" + CharsetUtil.UTF_8 + "\n")
                .append("\r\n")
                .append(message);

        // 将响应给客户端的内容写入到输出流
        serverOutputStream.write(sb.toString().getBytes(CharsetUtil.UTF_8));
        serverOutputStream.flush();
        serverOutputStream.close();
    }
}
