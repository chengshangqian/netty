package com.fandou.learning.netty.core.chapter4.server;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 抽象HttpServer：只做一些初始化工作
 */
public abstract class AbstractHttpServer implements HttpServer {

    /**
     * 默认配置文件web.xml路径：这里使用属性文件代替xml文件
     */
    private final static String DEFAULT_LOCATION = AbstractHttpServer.class.getResource("/").getPath() + "web.properties";

    /**
     * 监听端口
     */
    private final int port;

    /**
     * 配置文件web.xml：示例中使用属性文件代替
     */
    private final Properties configuration;

    /**
     * servlet映射：保存web.xml配置的servlet信息
     */
    private final Map<String, Servlet> servletMapping;

    /**
     * web.xml配置文件路径
     */
    private final String configurationLocation;

    /**
     * 初始化
     *
     * @param port 监听的端口
     */
    public AbstractHttpServer(int port) {
        this(port,DEFAULT_LOCATION);
    }

    /**
     * 初始化
     *
     * @param port 监听的端口
     * @param configurationLocation web.xml配置文件完整路径
     */
    public AbstractHttpServer(int port,String configurationLocation) {
        this.port = port;
        this.configurationLocation = configurationLocation;

        configuration = new Properties();
        servletMapping = new HashMap<String, Servlet>();

        // 初始化服务器
        init();
    }

    /**
     * 初始化服务器
     */
    private void init() {
        try{
            System.out.println("configurationLocation => " + this.configurationLocation);

            // 获取web.xml文件的输入流,加载web.xml内容到属性对象
            FileInputStream is = new FileInputStream(this.configurationLocation);
            configuration.load(is);
            is.close();

            // 解析web.xml
            for (Object k : configuration.keySet()) {
                // 属性key
                String key = k.toString();

                // 解析为Servlet：约定使用url结尾的key来定义servlet
                if(key.endsWith(".url")){
                    // 获取servlet的名称：属性key在.url之前的部分作为servlet名称
                    String servletName = key.replaceAll("\\.url$","");

                    // servlet的url
                    String url = configuration.getProperty(key);

                    // 处理servlet请求对应的servlet类
                    String className = configuration.getProperty(servletName + ".class");

                    // 创建servlet实例：整个容器中，将使用一个实例，即单实例，多线程
                    Servlet servlet = (Servlet)Class.forName(className).newInstance();

                    // 使用url作为key，保存servlet实例
                    servletMapping.put(url,servlet);
                }
            }
        }
        catch(Exception e){
            System.err.println("初始化服务器出错：");
            e.printStackTrace();
        }
    }

    /**
     * 获取Servlet映射集合
     *
     * @return
     */
    public Map<String, Servlet> getServletMapping() {
        return servletMapping;
    }

    /**
     * 获取监听的端口号
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * 启动服务器
     */
    @Override
    public void start(){
        doStart();
    }

    /**
     * 抽象模板方法：留给具体子类扩展实现
     */
    public abstract void doStart();
}
