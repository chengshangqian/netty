package com.fandou.learning.netty.core.chapter13.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatClientTest {

    @Test
    void testChatClient(){
        new ChatClient("Cover").connect("127.0.0.1",8080);
    }
}