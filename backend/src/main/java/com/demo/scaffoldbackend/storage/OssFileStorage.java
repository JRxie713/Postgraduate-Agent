package com.demo.scaffoldbackend.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.demo.scaffoldbackend.config.ChatProperties;
import org.springframework.beans.factory.DisposableBean;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;

/**
 * 阿里云 OSS 存储：bucket 私有，服务端生成 key，通过预签名 URL 限时下载。
 */
public class OssFileStorage implements FileStorage, DisposableBean {

    private final OSS client;
    private final String bucket;

    public OssFileStorage(ChatProperties.Oss config) {
        if (isBlank(config.getEndpoint()) || isBlank(config.getBucket())
                || isBlank(config.getAccessKeyId()) || isBlank(config.getAccessKeySecret())) {
            throw new IllegalStateException("chat.storage.type=oss 时必须配置 OSS_ENDPOINT / OSS_BUCKET / "
                    + "OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET");
        }
        this.bucket = config.getBucket();
        this.client = new OSSClientBuilder().build(config.getEndpoint(),
                config.getAccessKeyId(), config.getAccessKeySecret());
    }

    @Override
    public void store(InputStream in, long size, String contentType, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        if (contentType != null && !contentType.isBlank()) {
            metadata.setContentType(contentType);
        }
        client.putObject(bucket, key, in, metadata);
    }

    @Override
    public String downloadUrl(String key, String fileName, boolean inline, long expireSeconds) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        request.setExpiration(new Date(System.currentTimeMillis() + expireSeconds * 1000));
        if (fileName != null && !fileName.isBlank()) {
            String disposition = (inline ? "inline" : "attachment")
                    + "; filename*=UTF-8''"
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            request.addQueryParameter("response-content-disposition", disposition);
        }
        return client.generatePresignedUrl(request).toString();
    }

    @Override
    public boolean local() {
        return false;
    }

    @Override
    public Path resolveLocal(String key) {
        throw new UnsupportedOperationException("OSS 存储不支持本地文件读取");
    }

    @Override
    public void destroy() {
        client.shutdown();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
