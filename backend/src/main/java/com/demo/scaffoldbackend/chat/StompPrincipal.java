package com.demo.scaffoldbackend.chat;

import java.security.Principal;

/**
 * WebSocket 会话主体：name 为用户ID
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
