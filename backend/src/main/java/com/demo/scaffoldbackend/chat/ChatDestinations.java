package com.demo.scaffoldbackend.chat;

/**
 * STOMP 目的地常量
 */
public final class ChatDestinations {

    private ChatDestinations() {
    }

    /** 客户端发送前缀 */
    public static final String APP_PREFIX = "/app";

    /** 会话广播：/topic/session/{sessionId} */
    public static final String SESSION_TOPIC_PREFIX = "/topic/session/";

    /** 发送结果回执（点对点） */
    public static final String QUEUE_ACK = "/queue/ack";

    /** 会话级通知（新会话邀请等，点对点） */
    public static final String QUEUE_INBOX = "/queue/inbox";
}
