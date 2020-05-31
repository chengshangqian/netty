package com.fandou.learning.netty.core.chapter4.server;

/**
 * 实现service，GET/POST方法实现的扩展留给具体的子类
 */
public abstract class AbstractServlet implements Servlet {

    /**
     * 处理客户端HTTP请求：仅作分发
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @Override
    public void service(Request request, Response response) throws Exception {
        String method = request.getMethod();
        if("GET".equalsIgnoreCase(method)){
            doGet(request,response);
        }
        else if("POST".equalsIgnoreCase(method)){
            doPost(request,response);
        }
        else{
            throw new Exception("不支持的HTTP请求方法 => " + method);
        }
    }

    /**
     * 处理GET方法请求
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public abstract void doGet(Request request, Response response) throws Exception;

    /**
     * 处理POST方法请求
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public abstract void doPost(Request request, Response response) throws Exception;
}
