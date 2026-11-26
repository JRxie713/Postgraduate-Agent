package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.config.ChatProperties;
import com.demo.scaffoldbackend.config.JwtUtil;
import com.demo.scaffoldbackend.entity.ChatFile;
import com.demo.scaffoldbackend.mapper.ChatFileMapper;
import com.demo.scaffoldbackend.service.ChatFileService;
import com.demo.scaffoldbackend.service.ChatService;
import com.demo.scaffoldbackend.storage.FileStorage;
import com.demo.scaffoldbackend.vo.ChatFileUrlVO;
import com.demo.scaffoldbackend.vo.ChatFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 聊天附件服务实现：校验权限与大小，生成服务端 key 后写入对象存储
 */
@Service
@RequiredArgsConstructor
public class ChatFileServiceImpl implements ChatFileService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final ChatFileMapper fileMapper;
    private final ChatService chatService;
    private final FileStorage storage;
    private final ChatProperties properties;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public ChatFileVO upload(Integer userId, Long sessionId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        if (sessionId == null) {
            throw new BusinessException(400, "会话ID不能为空");
        }
        if (!chatService.isMember(sessionId, userId)) {
            throw new BusinessException(403, "你不在该会话中");
        }
        long size = file.getSize();
        String contentType = file.getContentType();
        boolean image = contentType != null && contentType.startsWith("image/");
        long limit = image ? properties.getImageMaxBytes() : properties.getFileMaxBytes();
        if (size > limit) {
            throw new BusinessException(413, image ? "图片不能超过 10MB" : "文件不能超过 100MB");
        }

        String fileName = safeFileName(file.getOriginalFilename());
        String key = "chat/" + sessionId + "/" + LocalDateTime.now().format(MONTH_FORMAT)
                + "/" + UUID.randomUUID().toString().replace("-", "") + extension(fileName);
        try (InputStream in = file.getInputStream()) {
            storage.store(in, size, contentType, key);
        } catch (IOException e) {
            throw new BusinessException(500, "读取上传文件失败");
        }

        ChatFile entity = new ChatFile();
        entity.setSessionId(sessionId);
        entity.setUploaderId(userId);
        entity.setFileName(fileName);
        entity.setFileSize(size);
        entity.setContentType(contentType);
        entity.setStorageKey(key);
        entity.setCreateTime(LocalDateTime.now());
        fileMapper.insert(entity);

        ChatFileVO vo = new ChatFileVO();
        vo.setId(entity.getId());
        vo.setSessionId(sessionId);
        vo.setFileName(fileName);
        vo.setFileSize(size);
        vo.setContentType(contentType);
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    @Override
    public ChatFileUrlVO url(Integer userId, Long fileId) {
        ChatFile file = requireFile(fileId);
        if (!chatService.isMember(file.getSessionId(), userId)) {
            throw new BusinessException(403, "你不在该会话中");
        }
        long expiresIn = properties.getStorage().getDownloadExpireSeconds();
        boolean inline = file.getContentType() != null && file.getContentType().startsWith("image/");
        String url = storage.downloadUrl(file.getStorageKey(), file.getFileName(), inline, expiresIn);
        return new ChatFileUrlVO(url, expiresIn);
    }

    @Override
    public Path localFile(String token) {
        if (!storage.local()) {
            throw new BusinessException(400, "当前存储模式不支持该下载方式");
        }
        String key;
        try {
            key = jwtUtil.parseFileTicket(token);
        } catch (Exception e) {
            throw new BusinessException(401, "下载链接已失效，请重新获取");
        }
        Path path = storage.resolveLocal(key);
        if (!Files.exists(path)) {
            throw new BusinessException(404, "文件不存在");
        }
        return path;
    }

    private ChatFile requireFile(Long fileId) {
        if (fileId == null) {
            throw new BusinessException(400, "文件ID不能为空");
        }
        ChatFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(404, "文件不存在");
        }
        return file;
    }

    private String safeFileName(String original) {
        String name = original == null ? "" : original.trim();
        if (name.isEmpty()) {
            return "file";
        }
        try {
            name = Paths.get(name).getFileName().toString();
        } catch (InvalidPathException e) {
            name = name.replaceAll("[\\\\/]", "_");
        }
        name = name.replaceAll("[\\r\\n\\t]", "").trim();
        if (name.isEmpty()) {
            return "file";
        }
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
        return ext.isEmpty() || ext.length() > 10 ? "" : "." + ext;
    }
}
