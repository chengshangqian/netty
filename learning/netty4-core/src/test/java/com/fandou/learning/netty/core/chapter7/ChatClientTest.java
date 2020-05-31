package com.fandou.learning.netty.core.chapter7;

import org.junit.jupiter.api.Test;

class ChatClientTest {
    @Test
    void testChatClient(){
        new ChatClient().connect("127.0.0.1",12346,"张三");
    }
}