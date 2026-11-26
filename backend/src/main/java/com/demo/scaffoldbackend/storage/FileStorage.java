package com.demo.scaffoldbackend.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 聊天附件存储抽象：local（本机磁盘）或 oss（阿里云对象存储）
 */
public interface FileStorage {

    /** 保存对象；key 由服务端生成，避免用户可控路径 */
    void store(InputStream in, long size, String contentType, String key);

    /** 生成带时效的下载地址 */
    String downloadUrl(String key, String fileName, boolean inline, long expireSeconds);

    /** 是否本机存储 */
    boolean local();

    /** 本机存储时把 key 解析为磁盘路径 */
    Path resolveLocal(String key);
}
