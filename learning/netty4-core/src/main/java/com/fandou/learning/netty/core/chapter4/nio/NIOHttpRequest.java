package com.fandou.learning.netty.core.chapter4.nio;

import com.fandou.learning.netty.core.chapter4.server.AbstractRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求
 */
public class NIOHttpRequest extends AbstractRequest {

    /**
     * Netty封装的客户端channel的请求上下文
     */
    private final ChannelHandlerContext ctx;

    /**
     * Netty封装的客户端的请求
     */
    private final HttpRequest request;


    /**
     * 初始化HTTP请求：内部封装了Netty实现的HttpRequest
     *
     * @param ctx Netty封装的客户端channel的请求上下文
     * @param request Netty封装的客户端的请求
     */
    public NIOHttpRequest(ChannelHandlerContext ctx, HttpRequest request){
        this.ctx = ctx;
        this.request = request;

        // 请求方法
        setMethod(request.method().name());

        // 包含请求参数的资源地址
        String uri = request.uri();
        String[] urlParams = uri.replaceAll("\\s","").split("\\?");
        // 请求URL即Servlet
        setUrl(urlParams[0]);

        // 获取请求的参数
        if(urlParams.length > 1 && null != urlParams[1] && !urlParams[1].isEmpty()){
            // 初始化参数值集
            Map<String,String> parameters = new HashMap<String,String>();

            // 参数名称和值数组集合：取出参数字符串中的所有空格后使用&分割
            String[] params =  urlParams[1].split("&");
            for(String param : params) {
                // 跳过无效的参数：[空]&name=value
                if(null == param || param.isEmpty()){
                    continue;
                }

                // 参数：名和值
                String[] nameValue = param.split("=");

                // 参数名
                String name = nameValue[0];
                // 跳过无效参数名：[=value]&name=value
                if(null == name || name.isEmpty()){
                    continue;
                }

                // 参数值：有可能是空，比如[name=]&paramName=paramVal
                String value = null;
                if(nameValue.length > 1){
                    value = nameValue[1];
                }

                // 去掉#号及其后面的串
                if (null != value && value.indexOf("#") > -1) {
                    value = value.substring(0,value.indexOf("#"));
                }

                // 保存到参数集合中
                parameters.put(name,value);
            }

            // 请求参数
            setParameters(parameters);
        }

        /*
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        if(null != params && !params.isEmpty()){
            // 初始化参数值集
            Map<String,String> parameters = new HashMap<>();
            for (String paramName: params.keySet()) {
                List<String> param = params.get(paramName);
                String value = null;
                if(null != param){
                    // 拿第1个作为值，这里仅作演示
                    value = param.get(0);
                }
                parameters.put(paramName,value);
            }
            setParameters(parameters);
        }
        */
    }

    @Override
    public String toString() {
        return "NIOHttpRequest{" +
                "method=" + getMethod() +
                ", url=" + getUrl() +
                ", parameters=" + getParameters() +
                '}';
    }
}
