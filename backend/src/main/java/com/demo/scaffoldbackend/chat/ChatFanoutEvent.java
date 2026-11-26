package com.demo.scaffoldbackend.chat;

import lombok.Data;

/**
 * 多实例广播事件：业务实例发布 -> Redis Pub/Sub -> 各实例本机 STOMP 扇出
 */
@Data
public class ChatFanoutEvent {

    /** SESSION：广播到 /topic/session/{sessionId}；USER：推送到用户私有队列 */
    private String kind;

    private Long sessionId;

    private Integer userId;

    /** 用户队列名：ack / inbox */
    private String queue;

    /** JSON 字符串载荷 */
    private String payload;
}
