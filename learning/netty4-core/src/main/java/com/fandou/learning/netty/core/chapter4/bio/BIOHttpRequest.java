package com.fandou.learning.netty.core.chapter4.bio;

import com.fandou.learning.netty.core.chapter4.server.AbstractRequest;
import io.netty.util.CharsetUtil;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求
 */
public class BIOHttpRequest extends AbstractRequest {

    /**
     * 初始化HTTP请求：根据HTTP协议的规范，解析客户端发起的HTTP请求（输入流）
     *
     * @param clientInputStream 客户端输入流
     */
    public BIOHttpRequest(InputStream clientInputStream)  {
        try{
            // Http请求输入流中的内容
            String content = "";

            // 缓冲：这里定义1024字节大小
            byte[] buff = new byte[1024];

            int len;

            // 从客户端输入流中读取1024字节内容，假设发送的内容不大于1024字节，仅作演示
            if((len = clientInputStream.read(buff)) > 0){
                content = new String(buff,0,len, CharsetUtil.UTF_8);
            }
            else {
                // 空内容：头部请求？
                return;
            }

            // 解析读取第一行内容：主要是请求的方法和资源路径
            // 第一行的内容大概如下：GET /user.do?param1=value1&param2=value2 HTTP/1.1\r\n
            String firstLine = content.split("\\n")[0];

            // 以空格分割行：{METHOD,URL,PROTOCOL/VERSION}
            String[] arr = firstLine.split("\\s");

            // 获取请求方法
            setMethod(arr[0].trim());

            // 请求资源路径和参数：去掉可能空格后使用问号?分割
            String[] urlParams = arr[1].replaceAll("\\s","").split("\\?");
            // 获取请求的资源路径即servlet
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

                setParameters(parameters);
            }
        }
        catch (Exception e){
            System.err.println("接收的HTTP请求解析出错:");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "BIOHttpRequest{" +
                "method=" + getMethod() +
                ", url=" + getUrl() +
                ", parameters=" + getParameters() +
                '}';
    }
}
