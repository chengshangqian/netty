package com.fandou.learning.netty.core.chapter4.servlet;

import com.fandou.learning.netty.core.chapter4.server.AbstractServlet;
import com.fandou.learning.netty.core.chapter4.server.Request;
import com.fandou.learning.netty.core.chapter4.server.Response;

/**
 * 处理用户相关的的Servlet
 */
public class UserServlet extends AbstractServlet {

    @Override
    public void doGet(Request request, Response response) throws Exception {
        this.doPost(request,response);
    }

    @Override
    public void doPost(Request request, Response response) throws Exception {
        response.write("用户服务...");
    }
}
