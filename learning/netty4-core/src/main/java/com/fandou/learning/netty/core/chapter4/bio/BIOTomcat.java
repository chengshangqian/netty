package com.fandou.learning.netty.core.chapter4.bio;

import com.fandou.learning.netty.core.chapter4.server.AbstractHttpServer;
import com.fandou.learning.netty.core.chapter4.server.Request;
import com.fandou.learning.netty.core.chapter4.server.Response;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 使用阻塞IO实现类似Tomcat的HTTP服务器
 */
public class BIOTomcat extends AbstractHttpServer {

    /**
     * 绑定Socket服务端：以监听网络HTTP请求
     */
    private ServerSocket server;

    /**
     * 构造器初始化一些成员
     *
     * @param port 初始化监听端口
     */
    public BIOTomcat(int port) {
        super(port);
    }

    /**
     * 启动服务器，监听客户端请求
     */
    @Override
    public void doStart() {
        // 启动服务器
        try {
            server = new ServerSocket(getPort());
            System.out.println("BIOTomcat启动,监听端口" + getPort());

            // 接收并处理客户端的请求
            for(;;){
                // 接收到客户端请求：server.accept()方法会一直阻塞，直到接收到请求
                Socket client = server.accept();

                // 处理客户端请求
                process(client);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * 处理客户端请求
     *
     * @param client 客户端
     */
    private void process(Socket client) throws Exception{
        // 获取客户端输入流
        InputStream is = client.getInputStream();

        // 获取客户端输出流
        OutputStream os = client.getOutputStream();

        try{
            // 创建Http请求和响应
            Request request = new BIOHttpRequest(is);
            Response response = new BIOHttpResponse(os);

            // 打印请求信息
            System.out.println("request => " + request);

            // 获取请求的资源路径即url
            String url = request.getUrl();

            if(null != url){
                // 调用对应的servlet处理响应客户端的请求
                if(getServletMapping().containsKey(url)){
                    getServletMapping().get(url).service(request,response);
                }
                // 如果请求的资源即url不存在，返回404
                else  if("/favicon.ico".equalsIgnoreCase(url)){
                    // TODO
                    response.write("404 - Not Found.");
                }
                else {
                    response.write("404 - Not Found.");
                }
            }
        }
        finally {
            // 清理资源
            os.flush();
            os.close();
            is.close();
            client.close();
        }
    }
}
