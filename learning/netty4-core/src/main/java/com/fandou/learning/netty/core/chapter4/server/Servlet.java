package com.fandou.learning.netty.core.chapter4.server;

public interface Servlet {
    public void service(Request request, Response response) throws Exception;
}
