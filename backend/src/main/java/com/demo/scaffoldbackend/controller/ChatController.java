package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.chat.ChatWsTicketService;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.config.ChatProperties;
import com.demo.scaffoldbackend.dto.ChatAddMembersDTO;
import com.demo.scaffoldbackend.dto.ChatGroupSessionDTO;
import com.demo.scaffoldbackend.dto.ChatPrivateSessionDTO;
import com.demo.scaffoldbackend.dto.ChatReadDTO;
import com.demo.scaffoldbackend.service.ChatFileService;
import com.demo.scaffoldbackend.service.ChatService;
import com.demo.scaffoldbackend.vo.ChatContactVO;
import com.demo.scaffoldbackend.vo.ChatFileUrlVO;
import com.demo.scaffoldbackend.vo.ChatFileVO;
import com.demo.scaffoldbackend.vo.ChatMemberVO;
import com.demo.scaffoldbackend.vo.ChatMessageVO;
import com.demo.scaffoldbackend.vo.ChatReadVO;
import com.demo.scaffoldbackend.vo.ChatSessionVO;
import com.demo.scaffoldbackend.vo.ChatWsTicketVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 实时聊天 REST 接口：会话、历史消息、已读、撤回、附件与 WebSocket 票据
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatFileService chatFileService;
    private final ChatWsTicketService ticketService;
    private final ChatProperties properties;

    // ============================== 连接票据 ==============================

    /** 签发一次性 WebSocket 连接票据 */
    @PostMapping("/ws-ticket")
    public Result<ChatWsTicketVO> wsTicket(Authentication authentication) {
        ChatWsTicketVO vo = new ChatWsTicketVO();
        vo.setTicket(ticketService.issue(currentUserId(authentication)));
        vo.setExpiresIn(properties.getTicketTtlSeconds());
        vo.setWsPath("/ws");
        return Result.success(vo);
    }

    // ============================== 会话 ==============================

    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> sessions(Authentication authentication) {
        return Result.success(chatService.listSessions(currentUserId(authentication)));
    }

    @PostMapping("/sessions/private")
    public Result<ChatSessionVO> privateSession(Authentication authentication,
                                                @RequestBody ChatPrivateSessionDTO dto) {
        return Result.success(chatService.createPrivateSession(currentUserId(authentication), dto.getPeerId()));
    }

    @PostMapping("/sessions/group")
    public Result<ChatSessionVO> groupSession(Authentication authentication,
                                              @RequestBody ChatGroupSessionDTO dto) {
        return Result.success(chatService.createGroupSession(currentUserId(authentication), dto));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionVO> session(Authentication authentication, @PathVariable Long sessionId) {
        return Result.success(chatService.getSession(currentUserId(authentication), sessionId));
    }

    @GetMapping("/sessions/{sessionId}/members")
    public Result<List<ChatMemberVO>> members(Authentication authentication, @PathVariable Long sessionId) {
        return Result.success(chatService.listMembers(currentUserId(authentication), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/members")
    public Result<Boolean> addMembers(Authentication authentication, @PathVariable Long sessionId,
                                      @RequestBody ChatAddMembersDTO dto) {
        chatService.addMembers(currentUserId(authentication), sessionId, dto.getMemberIds());
        return Result.success(true);
    }

    // ============================== 消息 ==============================

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> messages(Authentication authentication, @PathVariable Long sessionId,
                                                @RequestParam(required = false) Long afterId,
                                                @RequestParam(required = false) Long beforeId,
                                                @RequestParam(required = false, defaultValue = "30") Integer size) {
        return Result.success(chatService.history(currentUserId(authentication), sessionId, afterId, beforeId, size));
    }

    @PostMapping("/sessions/{sessionId}/read")
    public Result<ChatReadVO> read(Authentication authentication, @PathVariable Long sessionId,
                                   @RequestBody ChatReadDTO dto) {
        return Result.success(chatService.markRead(currentUserId(authentication), sessionId, dto.getLastReadMsgId()));
    }

    @PostMapping("/messages/{messageId}/recall")
    public Result<ChatMessageVO> recall(Authentication authentication, @PathVariable Long messageId) {
        return Result.success(chatService.recall(currentUserId(authentication), messageId));
    }

    // ============================== 联系人 ==============================

    @GetMapping("/contacts")
    public Result<List<ChatContactVO>> contacts(Authentication authentication) {
        return Result.success(chatService.contacts(currentUserId(authentication)));
    }

    // ============================== 附件 ==============================

    @PostMapping("/files")
    public Result<ChatFileVO> upload(Authentication authentication, @RequestParam Long sessionId,
                                     @RequestParam("file") MultipartFile file) {
        return Result.success(chatFileService.upload(currentUserId(authentication), sessionId, file));
    }

    @GetMapping("/files/{fileId}/url")
    public Result<ChatFileUrlVO> fileUrl(Authentication authentication, @PathVariable Long fileId) {
        return Result.success(chatFileService.url(currentUserId(authentication), fileId));
    }

    /**
     * 本机存储模式下的临时下载入口：凭 JWT 签名令牌访问，不依赖 Authorization 头。
     * OSS 模式不会生成该地址。
     */
    @GetMapping("/files/raw")
    public ResponseEntity<Resource> raw(@RequestParam String token,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false, defaultValue = "0") int inline) {
        Path path = chatFileService.localFile(token);
        String detected;
        try {
            detected = Files.probeContentType(path);
        } catch (Exception e) {
            detected = null;
        }
        MediaType mediaType = detected == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(detected);
        String fileName = name == null || name.isBlank() ? path.getFileName().toString() : name;
        ContentDisposition disposition = ContentDisposition
                .builder(inline == 1 ? "inline" : "attachment")
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(path));
    }

    private Integer currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(401, "未登录或令牌已失效");
        }
        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "非法用户身份");
        }
    }
}
