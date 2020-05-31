package com.fandou.learning.netty.core.chapter4.server;

import java.util.Map;

public interface Request {
    public String getMethod();

    public String getUrl();

    public Map<String, String> getParameters();

    public Object getParameter(String paramName);
}
