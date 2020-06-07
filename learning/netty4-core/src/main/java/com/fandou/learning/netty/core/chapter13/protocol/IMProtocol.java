package com.fandou.learning.netty.core.chapter13.protocol;

/**
 * 自定义即时通讯协议
 */
public enum IMProtocol {

    /**
     * 系统消息指令
     */
    SYSTEM("SYSTEM"),

    /**
     * 登录指令
     */
    LOGIN("LOGIN"),

    /**
     * 登出指令
     */
    LOGOUT("LOGOUT"),

    /**
     * 发送聊天消息指令
     */
    CHAT("CHAT"),

    /**
     * 送鲜花指令
     */
    FLOWER("FLOWER");

    /**
     * 枚举类的名称
     */
    private String name;

    /**
     * 给定名称创建一个即时通讯协议IMProtocol枚举类
     *
     * @param name 枚举类的名称
     */
    IMProtocol(String name){
        this.name = name;
    }

    /**
     * 是否是协议指令开头的内容
     *
     * @param content
     * @return
     */
    public static boolean startWithIMProtocol(String content){
        return content.matches("^\\[(SYSTEM|LOGIN|LOGOUT|CHAT|FLOWER)\\]");
    }

    /**
     * 获取协议指令名称
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 发送系统消息
     *
     * @param cmd
     * @return
     */
    public static boolean isSystem(String cmd){
        return isIMProtocol(IMProtocol.SYSTEM,cmd);
    }

    /**
     * 发送登录指令
     *
     * @param cmd
     * @return
     */
    public static boolean isLogin(String cmd){
        return isIMProtocol(IMProtocol.LOGIN,cmd);
    }

    /**
     * 发送登出指令
     *
     * @param cmd
     * @return
     */
    public static boolean isLogout(String cmd){
        return isIMProtocol(IMProtocol.LOGOUT,cmd);
    }

    /**
     * 发送聊天信息
     *
     * @param cmd
     * @return
     */
    public static boolean isChat(String cmd){
        return isIMProtocol(IMProtocol.CHAT,cmd);
    }

    /**
     * 发送鲜花
     *
     * @param cmd
     * @return
     */
    public static boolean isFlower(String cmd){
        return isIMProtocol(IMProtocol.FLOWER,cmd);
    }

    /**
     * 验证指令是否是指定的协议指令
     *
     * @param protocol 指定的协议指令
     * @param cmd 要验证的指令
     * @return
     */
    private static boolean isIMProtocol(IMProtocol protocol, String cmd){

        if(null == cmd || protocol == null){
            return false;
        }

        return protocol.getName().equals(cmd);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
