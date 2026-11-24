package com.demo.scaffoldbackend.storage;

import com.demo.scaffoldbackend.config.ChatProperties;
import com.demo.scaffoldbackend.config.JwtUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按 chat.storage.type 选择文件存储实现
 */
@Configuration
public class FileStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "chat.storage.type", havingValue = "oss")
    public FileStorage ossFileStorage(ChatProperties properties) {
        return new OssFileStorage(properties.getOss());
    }

    @Bean
    @ConditionalOnProperty(name = "chat.storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(ChatProperties properties, JwtUtil jwtUtil) {
        return new LocalFileStorage(properties.getStorage().getLocalDir(), jwtUtil);
    }
}
