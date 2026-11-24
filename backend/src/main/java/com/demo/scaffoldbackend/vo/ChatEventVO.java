package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * WebSocket 推送事件统一包装
 */
@Data
public class ChatEventVO {

    /** MESSAGE / READ / RECALL / TYPING / SESSION_CREATED */
    private String event;

    private ChatMessageVO message;

    private ChatReadVO read;

    private ChatRecallVO recall;

    private ChatTypingVO typing;

    private ChatSessionVO session;

    public static ChatEventVO message(ChatMessageVO message) {
        ChatEventVO event = new ChatEventVO();
        event.setEvent("MESSAGE");
        event.setMessage(message);
        return event;
    }

    public static ChatEventVO read(ChatReadVO read) {
        ChatEventVO event = new ChatEventVO();
        event.setEvent("READ");
        event.setRead(read);
        return event;
    }

    public static ChatEventVO recall(ChatRecallVO recall) {
        ChatEventVO event = new ChatEventVO();
        event.setEvent("RECALL");
        event.setRecall(recall);
        return event;
    }

    public static ChatEventVO typing(ChatTypingVO typing) {
        ChatEventVO event = new ChatEventVO();
        event.setEvent("TYPING");
        event.setTyping(typing);
        return event;
    }

    public static ChatEventVO sessionCreated(ChatSessionVO session) {
        ChatEventVO event = new ChatEventVO();
        event.setEvent("SESSION_CREATED");
        event.setSession(session);
        return event;
    }
}
