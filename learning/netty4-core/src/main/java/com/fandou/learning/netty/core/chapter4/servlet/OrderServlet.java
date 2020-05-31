package com.fandou.learning.netty.core.chapter4.servlet;

import com.fandou.learning.netty.core.chapter4.server.AbstractServlet;
import com.fandou.learning.netty.core.chapter4.server.Request;
import com.fandou.learning.netty.core.chapter4.server.Response;

/**
 * 处理订单相关的的Servlet
 */
public class OrderServlet extends AbstractServlet {

    public void doGet(Request request, Response response) throws Exception {
        this.doPost(request,response);
    }

    public void doPost(Request request, Response response) throws Exception {
        response.write("订单服务...");
    }
}
