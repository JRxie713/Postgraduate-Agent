package com.demo.scaffoldbackend.chat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.security.Principal;

/**
 * 原生 WebSocket + STOMP 配置
 *
 * <p>连接地址：/ws?ticket=xxx（ticket 由 /api/chat/ws-ticket 签发，一次性、60 秒有效）。
 * 客户端发送前缀 /app，订阅 /topic/session/{id} 与 /user/queue/**。</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatWsTicketService ticketService;
    private final StompAuthChannelInterceptor authChannelInterceptor;

    public WebSocketConfig(ChatWsTicketService ticketService, StompAuthChannelInterceptor authChannelInterceptor) {
        this.ticketService = ticketService;
        this.authChannelInterceptor = authChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WsTicketHandshakeInterceptor(ticketService))
                .setHandshakeHandler(new org.springframework.web.socket.server.support.DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                                      org.springframework.web.socket.WebSocketHandler wsHandler,
                                                      java.util.Map<String, Object> attributes) {
                        Object userId = attributes.get(WsTicketHandshakeInterceptor.ATTR_USER_ID);
                        return userId == null ? null : new StompPrincipal(String.valueOf(userId));
                    }
                });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 简单内存 broker：/topic 广播、/queue 点对点（用户队列）
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes(ChatDestinations.APP_PREFIX);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024)
                .setSendBufferSizeLimit(1024 * 1024)
                .setSendTimeLimit(20000);
    }

    /** 放宽容器缓冲区，避免较长文本或群成员列表被截断 */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(128 * 1024);
        container.setMaxBinaryMessageBufferSize(128 * 1024);
        return container;
    }
}
