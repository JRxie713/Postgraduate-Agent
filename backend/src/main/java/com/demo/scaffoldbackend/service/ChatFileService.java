package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.vo.ChatFileUrlVO;
import com.demo.scaffoldbackend.vo.ChatFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 聊天附件服务
 */
public interface ChatFileService {

    ChatFileVO upload(Integer userId, Long sessionId, MultipartFile file);

    ChatFileUrlVO url(Integer userId, Long fileId);

    /** 本机存储模式下解析临时下载令牌 */
    Path localFile(String token);
}
