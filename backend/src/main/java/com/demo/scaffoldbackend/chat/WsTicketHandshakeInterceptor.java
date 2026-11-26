package com.demo.scaffoldbackend.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 握手拦截：从 /ws?ticket=xxx 中取出一次性票据并换取用户ID
 */
public class WsTicketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "chatUserId";

    private final ChatWsTicketService ticketService;

    public WsTicketHandshakeInterceptor(ChatWsTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Integer userId = ticketService.consume(extractTicket(request.getURI().getQuery()));
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATTR_USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    private String extractTicket(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            if (pair.startsWith("ticket=")) {
                return URLDecoder.decode(pair.substring("ticket=".length()), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
