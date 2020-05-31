package com.fandou.learning.netty.core.chapter4.server;

import java.util.Map;

/**
 * 提供一些公共实现
 */
public abstract class AbstractRequest implements Request {

    // 请求的方法
    private String method;

    // 请求的资源路径
    private String url;

    // 请求参数值集
    private Map<String,String> parameters;

    protected void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getMethod() {
        return method;
    }

    protected void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUrl() {
        return url;
    }

    protected void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * 获取单个参数的值
     *
     * @param paramName
     * @return
     */
    @Override
    public Object getParameter(String paramName){
        if(null == parameters){
            return null;
        }
        return parameters.get(paramName);
    }
}
