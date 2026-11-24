package com.demo.scaffoldbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 实时聊天配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    /** 群聊/自习室人数上限 */
    private int groupMaxMembers = 200;

    /** 图片大小上限（字节） */
    private long imageMaxBytes = 10L * 1024 * 1024;

    /** 文件大小上限（字节） */
    private long fileMaxBytes = 100L * 1024 * 1024;

    /** 消息可撤回时限（秒） */
    private int recallSeconds = 120;

    /** WebSocket 一次性票据有效期（秒） */
    private int ticketTtlSeconds = 60;

    /** 多实例 Redis Pub/Sub 广播频道 */
    private String fanoutChannel = "chat:fanout";

    private Storage storage = new Storage();

    private Oss oss = new Oss();

    @Data
    public static class Storage {

        /** local：本机磁盘；oss：阿里云对象存储 */
        private String type = "local";

        /** 本机存储目录 */
        private String localDir = "./data/chat-files";

        /** 下载地址有效期（秒） */
        private long downloadExpireSeconds = 600;
    }

    @Data
    public static class Oss {

        private String endpoint;

        private String bucket;

        private String accessKeyId;

        private String accessKeySecret;
    }
}
