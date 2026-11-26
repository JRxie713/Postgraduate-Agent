package com.demo.scaffoldbackend.storage;

import com.demo.scaffoldbackend.config.JwtUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本机磁盘存储：仅适合单实例本地开发。
 *
 * <p>下载地址带 JWT 签名令牌，有效期短，解决 &lt;img&gt; 无法携带 Authorization 头的问题。</p>
 */
public class LocalFileStorage implements FileStorage {

    private final Path root;
    private final JwtUtil jwtUtil;

    public LocalFileStorage(String localDir, JwtUtil jwtUtil) {
        this.root = Paths.get(localDir).toAbsolutePath().normalize();
        this.jwtUtil = jwtUtil;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("创建聊天文件目录失败：" + root, e);
        }
    }

    @Override
    public void store(InputStream in, long size, String contentType, String key) {
        Path target = resolveLocal(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("保存聊天文件失败：" + key, e);
        }
    }

    @Override
    public String downloadUrl(String key, String fileName, boolean inline, long expireSeconds) {
        String token = jwtUtil.issueFileTicket(key, expireSeconds);
        StringBuilder url = new StringBuilder("/api/chat/files/raw?token=").append(token);
        if (fileName != null && !fileName.isBlank()) {
            url.append("&name=").append(URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        }
        if (inline) {
            url.append("&inline=1");
        }
        return url.toString();
    }

    @Override
    public boolean local() {
        return true;
    }

    @Override
    public Path resolveLocal(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        return path;
    }
}
