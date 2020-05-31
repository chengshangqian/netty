package com.fandou.learning.netty.core.chapter7;

import org.junit.jupiter.api.Test;

class ChatServerTest {
    @Test
    void testChatServer() throws Exception {
        new ChatServer().start(12346);
    }
}