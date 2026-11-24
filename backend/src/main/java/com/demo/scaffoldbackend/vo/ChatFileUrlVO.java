package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 附件临时下载地址
 */
@Data
public class ChatFileUrlVO {

    private String url;

    private long expiresIn;

    public ChatFileUrlVO(String url, long expiresIn) {
        this.url = url;
        this.expiresIn = expiresIn;
    }
}
