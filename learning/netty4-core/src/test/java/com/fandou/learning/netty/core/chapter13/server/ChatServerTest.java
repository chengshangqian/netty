package com.fandou.learning.netty.core.chapter13.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {

    @Test
    void testChatServer(){
        new ChatServer().start(8080);
    }
}