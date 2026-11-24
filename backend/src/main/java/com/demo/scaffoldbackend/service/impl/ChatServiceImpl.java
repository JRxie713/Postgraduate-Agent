package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.scaffoldbackend.chat.ChatFanoutPublisher;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.config.ChatProperties;
import com.demo.scaffoldbackend.dto.ChatGroupSessionDTO;
import com.demo.scaffoldbackend.dto.ChatSendDTO;
import com.demo.scaffoldbackend.entity.ChatFile;
import com.demo.scaffoldbackend.entity.ChatMessage;
import com.demo.scaffoldbackend.entity.ChatSession;
import com.demo.scaffoldbackend.entity.ChatSessionMember;
import com.demo.scaffoldbackend.entity.User;
import com.demo.scaffoldbackend.mapper.ChatFileMapper;
import com.demo.scaffoldbackend.mapper.ChatMessageMapper;
import com.demo.scaffoldbackend.mapper.ChatSessionMapper;
import com.demo.scaffoldbackend.mapper.ChatSessionMemberMapper;
import com.demo.scaffoldbackend.mapper.UserMapper;
import com.demo.scaffoldbackend.service.ChatService;
import com.demo.scaffoldbackend.vo.ChatAckVO;
import com.demo.scaffoldbackend.vo.ChatContactVO;
import com.demo.scaffoldbackend.vo.ChatEventVO;
import com.demo.scaffoldbackend.vo.ChatMemberVO;
import com.demo.scaffoldbackend.vo.ChatMessageVO;
import com.demo.scaffoldbackend.vo.ChatReadVO;
import com.demo.scaffoldbackend.vo.ChatRecallVO;
import com.demo.scaffoldbackend.vo.ChatSessionVO;
import com.demo.scaffoldbackend.vo.ChatTypingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 聊天核心服务实现
 *
 * <p>消息先落库，事务提交后再通过 Redis Pub/Sub 广播，保证数据库是唯一事实来源；
 * 客户端重连时用 afterId 游标补拉，允许广播偶发丢失。</p>
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String TYPE_PRIVATE = "PRIVATE";
    private static final String TYPE_GROUP = "GROUP";
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final String MSG_TEXT = "TEXT";
    private static final String MSG_IMAGE = "IMAGE";
    private static final String MSG_FILE = "FILE";
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_RECALLED = "RECALLED";

    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatFileMapper fileMapper;
    private final UserMapper userMapper;
    private final ChatProperties properties;
    private final ChatFanoutPublisher publisher;

    // ============================== 会话 ==============================

    @Override
    public List<ChatSessionVO> listSessions(Integer userId) {
        List<ChatSessionMember> myMembers = memberMapper.selectList(Wrappers.<ChatSessionMember>lambdaQuery()
                .eq(ChatSessionMember::getUserId, userId));
        if (myMembers.isEmpty()) {
            return List.of();
        }
        Map<Long, ChatSessionMember> memberBySession = myMembers.stream()
                .collect(Collectors.toMap(ChatSessionMember::getSessionId, Function.identity(), (a, b) -> a));

        List<ChatSession> sessions = sessionMapper.selectBatchIds(memberBySession.keySet());
        if (sessions.isEmpty()) {
            return List.of();
        }

        // 私聊：批量取对方成员，避免 N+1
        List<Long> privateIds = sessions.stream()
                .filter(s -> TYPE_PRIVATE.equals(s.getType()))
                .map(ChatSession::getId)
                .toList();
        Map<Long, Integer> peerIdBySession = new HashMap<>();
        if (!privateIds.isEmpty()) {
            List<ChatSessionMember> peers = memberMapper.selectList(Wrappers.<ChatSessionMember>lambdaQuery()
                    .in(ChatSessionMember::getSessionId, privateIds)
                    .ne(ChatSessionMember::getUserId, userId));
            for (ChatSessionMember peer : peers) {
                peerIdBySession.put(peer.getSessionId(), peer.getUserId());
            }
        }
        Map<Integer, User> users = loadUsers(peerIdBySession.values());

        List<ChatSessionVO> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            ChatSessionVO vo = baseVO(session, memberBySession.get(session.getId()));
            if (TYPE_PRIVATE.equals(session.getType())) {
                Integer peerId = peerIdBySession.get(session.getId());
                applyPeer(vo, peerId, peerId == null ? null : users.get(peerId));
            }
            result.add(vo);
        }
        result.sort(Comparator.comparing(ChatSessionVO::getLastMsgTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
    @Transactional
    public ChatSessionVO createPrivateSession(Integer userId, Integer peerId) {
        if (peerId == null || peerId.equals(userId)) {
            throw new BusinessException(400, "请选择正确的聊天对象");
        }
        User peer = userMapper.selectById(peerId);
        if (peer == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String pairKey = Math.min(userId, peerId) + ":" + Math.max(userId, peerId);
        ChatSession session = sessionMapper.selectOne(Wrappers.<ChatSession>lambdaQuery()
                .eq(ChatSession::getPairKey, pairKey));
        if (session != null) {
            return getSession(userId, session.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        session = new ChatSession();
        session.setType(TYPE_PRIVATE);
        session.setPairKey(pairKey);
        session.setMemberCount(2);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        try {
            sessionMapper.insert(session);
        } catch (DuplicateKeyException e) {
            // 并发创建同一对私聊：唯一键兜底，回读已存在的会话
            ChatSession existing = sessionMapper.selectOne(Wrappers.<ChatSession>lambdaQuery()
                    .eq(ChatSession::getPairKey, pairKey));
            if (existing == null) {
                throw e;
            }
            return getSession(userId, existing.getId());
        }
        memberMapper.insert(newMember(session.getId(), userId, ROLE_MEMBER, now));
        memberMapper.insert(newMember(session.getId(), peerId, ROLE_MEMBER, now));

        ChatSessionVO vo = baseVO(session, null);
        applyPeer(vo, peerId, peer);
        afterCommit(() -> publisher.toUser(peerId, "inbox", ChatEventVO.sessionCreated(vo)));
        return vo;
    }

    @Override
    @Transactional
    public ChatSessionVO createGroupSession(Integer userId, ChatGroupSessionDTO dto) {
        String name = dto == null || dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException(400, "请填写群聊名称");
        }
        if (name.length() > 64) {
            throw new BusinessException(400, "群聊名称不能超过 64 个字符");
        }
        Set<Integer> memberIds = dto.getMemberIds() == null ? new LinkedHashSet<>()
                : dto.getMemberIds().stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        memberIds.remove(userId);
        if (memberIds.size() + 1 > properties.getGroupMaxMembers()) {
            throw new BusinessException(400, "群成员不能超过 " + properties.getGroupMaxMembers() + " 人");
        }
        if (!memberIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(memberIds);
            if (users.size() != memberIds.size()) {
                throw new BusinessException(400, "部分成员不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setType(TYPE_GROUP);
        session.setName(name);
        session.setOwnerId(userId);
        session.setMemberCount(memberIds.size() + 1);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);

        memberMapper.insert(newMember(session.getId(), userId, ROLE_OWNER, now));
        for (Integer memberId : memberIds) {
            memberMapper.insert(newMember(session.getId(), memberId, ROLE_MEMBER, now));
        }

        ChatSessionVO vo = baseVO(session, null);
        afterCommit(() -> {
            for (Integer memberId : memberIds) {
                publisher.toUser(memberId, "inbox", ChatEventVO.sessionCreated(vo));
            }
        });
        return vo;
    }

    @Override
    public ChatSessionVO getSession(Integer userId, Long sessionId) {
        ChatSession session = requireSession(sessionId);
        ChatSessionMember member = requireMember(sessionId, userId);
        ChatSessionVO vo = baseVO(session, member);
        if (TYPE_PRIVATE.equals(session.getType())) {
            ChatSessionMember peer = memberMapper.selectOne(Wrappers.<ChatSessionMember>lambdaQuery()
                    .eq(ChatSessionMember::getSessionId, sessionId)
                    .ne(ChatSessionMember::getUserId, userId));
            if (peer != null) {
                applyPeer(vo, peer.getUserId(), userMapper.selectById(peer.getUserId()));
            }
        }
        return vo;
    }

    @Override
    public List<ChatMemberVO> listMembers(Integer userId, Long sessionId) {
        requireMember(sessionId, userId);
        List<ChatSessionMember> members = memberMapper.selectList(Wrappers.<ChatSessionMember>lambdaQuery()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .orderByAsc(ChatSessionMember::getJoinTime));
        Map<Integer, User> users = loadUsers(members.stream().map(ChatSessionMember::getUserId).toList());
        List<ChatMemberVO> result = new ArrayList<>();
        for (ChatSessionMember member : members) {
            User user = users.get(member.getUserId());
            ChatMemberVO vo = new ChatMemberVO();
            vo.setUserId(member.getUserId());
            vo.setMemberRole(member.getMemberRole());
            if (user != null) {
                vo.setName(displayName(user));
                vo.setUserRole(user.getUserRole());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void addMembers(Integer userId, Long sessionId, List<Integer> memberIds) {
        ChatSession session = requireSession(sessionId);
        requireMember(sessionId, userId);
        if (!TYPE_GROUP.equals(session.getType())) {
            throw new BusinessException(400, "私聊不能添加成员");
        }
        if (!userId.equals(session.getOwnerId())) {
            throw new BusinessException(403, "只有群主可以添加成员");
        }
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BusinessException(400, "请选择要添加的成员");
        }
        Set<Integer> candidates = memberIds.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        candidates.remove(userId);
        Set<Integer> existing = memberMapper.selectList(Wrappers.<ChatSessionMember>lambdaQuery()
                        .eq(ChatSessionMember::getSessionId, sessionId))
                .stream().map(ChatSessionMember::getUserId).collect(Collectors.toSet());
        candidates.removeAll(existing);
        if (candidates.isEmpty()) {
            return;
        }
        int max = properties.getGroupMaxMembers();
        if (existing.size() + candidates.size() > max) {
            throw new BusinessException(400, "群成员不能超过 " + max + " 人");
        }
        if (userMapper.selectBatchIds(candidates).size() != candidates.size()) {
            throw new BusinessException(400, "部分成员不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        for (Integer memberId : candidates) {
            memberMapper.insert(newMember(sessionId, memberId, ROLE_MEMBER, now));
        }
        session.setMemberCount(existing.size() + candidates.size());
        session.setUpdateTime(now);
        sessionMapper.updateById(session);

        ChatSessionVO vo = baseVO(session, null);
        afterCommit(() -> {
            for (Integer memberId : candidates) {
                publisher.toUser(memberId, "inbox", ChatEventVO.sessionCreated(vo));
            }
        });
    }

    // ============================== 消息 ==============================

    @Override
    public List<ChatMessageVO> history(Integer userId, Long sessionId, Long afterId, Long beforeId, Integer size) {
        requireMember(sessionId, userId);
        int limit = size == null ? 30 : Math.max(1, Math.min(size, 100));

        List<ChatMessage> messages;
        if (afterId != null) {
            messages = messageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                    .eq(ChatMessage::getSessionId, sessionId)
                    .gt(ChatMessage::getId, afterId)
                    .orderByAsc(ChatMessage::getId)
                    .last("LIMIT " + limit));
        } else {
            messages = messageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                    .eq(ChatMessage::getSessionId, sessionId)
                    .lt(beforeId != null, ChatMessage::getId, beforeId)
                    .orderByDesc(ChatMessage::getId)
                    .last("LIMIT " + limit));
            Collections.reverse(messages);
        }
        return toVOList(messages);
    }

    @Override
    @Transactional
    public ChatMessageVO send(Integer userId, ChatSendDTO dto) {
        if (dto == null || dto.getSessionId() == null) {
            throw new BusinessException(400, "会话ID不能为空");
        }
        String clientMsgId = dto.getClientMsgId();
        if (clientMsgId == null || clientMsgId.isBlank() || clientMsgId.length() > 64) {
            throw new BusinessException(400, "clientMsgId 不合法");
        }
        ChatSession session = requireSession(dto.getSessionId());
        requireMember(session.getId(), userId);

        // 幂等：同一会话内相同 clientMsgId 只落一条
        ChatMessage existing = messageMapper.selectOne(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, session.getId())
                .eq(ChatMessage::getClientMsgId, clientMsgId));
        if (existing != null) {
            ChatMessageVO vo = toVOList(List.of(existing)).get(0);
            ChatAckVO ack = ack(clientMsgId, true, "消息已发送", existing.getId());
            afterCommit(() -> publisher.toUser(userId, "ack", ack));
            return vo;
        }

        String msgType = dto.getMsgType() == null || dto.getMsgType().isBlank()
                ? MSG_TEXT : dto.getMsgType().trim().toUpperCase();
        ChatFile file = null;
        String content = null;
        if (MSG_TEXT.equals(msgType)) {
            content = dto.getContent() == null ? "" : dto.getContent().trim();
            if (content.isEmpty()) {
                throw new BusinessException(400, "消息内容不能为空");
            }
            if (content.length() > 2000) {
                throw new BusinessException(400, "消息内容不能超过 2000 个字符");
            }
        } else if (MSG_IMAGE.equals(msgType) || MSG_FILE.equals(msgType)) {
            if (dto.getFileId() == null) {
                throw new BusinessException(400, "请先上传附件");
            }
            file = fileMapper.selectById(dto.getFileId());
            if (file == null || !session.getId().equals(file.getSessionId())) {
                throw new BusinessException(404, "附件不存在");
            }
            if (!userId.equals(file.getUploaderId())) {
                throw new BusinessException(403, "不能发送他人上传的附件");
            }
            if (MSG_IMAGE.equals(msgType)
                    && (file.getContentType() == null || !file.getContentType().startsWith("image/"))) {
                throw new BusinessException(400, "该附件不是图片");
            }
        } else {
            throw new BusinessException(400, "不支持的消息类型：" + msgType);
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(userId);
        message.setClientMsgId(clientMsgId);
        message.setMsgType(msgType);
        message.setContent(content);
        message.setFileId(file == null ? null : file.getId());
        message.setStatus(STATUS_NORMAL);
        message.setCreateTime(now);
        messageMapper.insert(message);

        String preview = preview(msgType, content, file);
        sessionMapper.update(null, Wrappers.<ChatSession>lambdaUpdate()
                .eq(ChatSession::getId, session.getId())
                .set(ChatSession::getLastMsgId, message.getId())
                .set(ChatSession::getLastMsgPreview, preview)
                .set(ChatSession::getLastMsgTime, now)
                .set(ChatSession::getUpdateTime, now));

        // 发送者自己的已读游标推进到最新消息
        memberMapper.update(null, Wrappers.<ChatSessionMember>lambdaUpdate()
                .eq(ChatSessionMember::getSessionId, session.getId())
                .eq(ChatSessionMember::getUserId, userId)
                .set(ChatSessionMember::getLastReadMsgId, message.getId())
                .set(ChatSessionMember::getUnreadCount, 0));
        // 其他成员未读数 +1
        memberMapper.update(null, Wrappers.<ChatSessionMember>lambdaUpdate()
                .eq(ChatSessionMember::getSessionId, session.getId())
                .ne(ChatSessionMember::getUserId, userId)
                .setSql("unread_count = unread_count + 1"));

        User sender = userMapper.selectById(userId);
        ChatMessageVO vo = toVO(message, sender, file);
        ChatAckVO ack = ack(clientMsgId, true, "消息已发送", message.getId());
        afterCommit(() -> {
            publisher.toSession(session.getId(), ChatEventVO.message(vo));
            publisher.toUser(userId, "ack", ack);
        });
        return vo;
    }

    @Override
    @Transactional
    public ChatReadVO markRead(Integer userId, Long sessionId, Long lastReadMsgId) {
        ChatSession session = requireSession(sessionId);
        ChatSessionMember member = requireMember(sessionId, userId);
        if (lastReadMsgId == null || lastReadMsgId <= 0) {
            throw new BusinessException(400, "已读消息ID不合法");
        }
        ChatMessage message = messageMapper.selectById(lastReadMsgId);
        if (message == null || !sessionId.equals(message.getSessionId())) {
            throw new BusinessException(400, "消息不属于该会话");
        }

        long current = member.getLastReadMsgId() == null ? 0 : member.getLastReadMsgId();
        long target = Math.max(current, lastReadMsgId);
        memberMapper.update(null, Wrappers.<ChatSessionMember>lambdaUpdate()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId)
                .set(ChatSessionMember::getLastReadMsgId, target)
                .set(ChatSessionMember::getUnreadCount, 0));

        ChatReadVO vo = new ChatReadVO();
        vo.setSessionId(sessionId);
        vo.setUserId(userId);
        vo.setLastReadMsgId(target);
        vo.setMemberCount(session.getMemberCount());
        if (TYPE_GROUP.equals(session.getType())) {
            Long readCount = memberMapper.selectCount(Wrappers.<ChatSessionMember>lambdaQuery()
                    .eq(ChatSessionMember::getSessionId, sessionId)
                    .ge(ChatSessionMember::getLastReadMsgId, target));
            vo.setReadCount(readCount == null ? 0 : readCount.intValue());
        }
        afterCommit(() -> publisher.toSession(sessionId, ChatEventVO.read(vo)));
        return vo;
    }

    @Override
    @Transactional
    public ChatMessageVO recall(Integer userId, Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        ChatSession session = requireSession(message.getSessionId());
        requireMember(message.getSessionId(), userId);
        if (!userId.equals(message.getSenderId())) {
            throw new BusinessException(403, "只能撤回自己发送的消息");
        }
        if (STATUS_RECALLED.equals(message.getStatus())) {
            return toVO(message, userMapper.selectById(message.getSenderId()), null);
        }
        LocalDateTime deadline = message.getCreateTime().plusSeconds(properties.getRecallSeconds());
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(400, "超过 " + properties.getRecallSeconds() + " 秒的消息不能撤回");
        }

        message.setStatus(STATUS_RECALLED);
        message.setRecallTime(LocalDateTime.now());
        messageMapper.updateById(message);

        if (message.getId().equals(session.getLastMsgId())) {
            sessionMapper.update(null, Wrappers.<ChatSession>lambdaUpdate()
                    .eq(ChatSession::getId, session.getId())
                    .set(ChatSession::getLastMsgPreview, "[消息已撤回]"));
        }

        ChatMessageVO vo = toVO(message, userMapper.selectById(message.getSenderId()), null);
        ChatRecallVO recall = new ChatRecallVO();
        recall.setSessionId(session.getId());
        recall.setMessageId(message.getId());
        recall.setOperatorId(userId);
        afterCommit(() -> publisher.toSession(session.getId(), ChatEventVO.recall(recall)));
        return vo;
    }

    @Override
    public void typing(Integer userId, Long sessionId) {
        requireMember(sessionId, userId);
        User user = userMapper.selectById(userId);
        ChatTypingVO vo = new ChatTypingVO();
        vo.setSessionId(sessionId);
        vo.setUserId(userId);
        vo.setName(user == null ? "有人" : displayName(user));
        publisher.toSession(sessionId, ChatEventVO.typing(vo));
    }

    @Override
    public boolean isMember(Long sessionId, Integer userId) {
        if (sessionId == null || userId == null) {
            return false;
        }
        Long count = memberMapper.selectCount(Wrappers.<ChatSessionMember>lambdaQuery()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId));
        return count != null && count > 0;
    }

    @Override
    public List<ChatContactVO> contacts(Integer userId) {
        User me = userMapper.selectById(userId);
        if (me == null) {
            throw new BusinessException(404, "用户不存在");
        }
        var query = Wrappers.<User>lambdaQuery().ne(User::getId, userId);
        if ("STUDENT".equals(me.getUserRole())) {
            query.eq(User::getUserRole, "TEACHER");
        } else if ("TEACHER".equals(me.getUserRole())) {
            query.eq(User::getUserRole, "STUDENT");
        }
        query.last("LIMIT 200");
        List<User> users = userMapper.selectList(query);
        List<ChatContactVO> result = new ArrayList<>();
        for (User user : users) {
            ChatContactVO vo = new ChatContactVO();
            vo.setId(user.getId());
            vo.setName(displayName(user));
            vo.setUserRole(user.getUserRole());
            result.add(vo);
        }
        return result;
    }

    // ============================== 私有辅助 ==============================

    private ChatSession requireSession(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(400, "会话ID不能为空");
        }
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        return session;
    }

    private ChatSessionMember requireMember(Long sessionId, Integer userId) {
        ChatSessionMember member = memberMapper.selectOne(Wrappers.<ChatSessionMember>lambdaQuery()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(403, "你不在该会话中");
        }
        return member;
    }

    private ChatSessionMember newMember(Long sessionId, Integer userId, String role, LocalDateTime joinTime) {
        ChatSessionMember member = new ChatSessionMember();
        member.setSessionId(sessionId);
        member.setUserId(userId);
        member.setMemberRole(role);
        member.setLastReadMsgId(0L);
        member.setUnreadCount(0);
        member.setJoinTime(joinTime);
        return member;
    }

    private ChatSessionVO baseVO(ChatSession session, ChatSessionMember viewer) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setType(session.getType());
        vo.setName(TYPE_GROUP.equals(session.getType()) && (session.getName() == null || session.getName().isBlank())
                ? "自习室" : session.getName());
        vo.setOwnerId(session.getOwnerId());
        vo.setLastMsgId(session.getLastMsgId());
        vo.setLastMsgPreview(session.getLastMsgPreview());
        vo.setLastMsgTime(session.getLastMsgTime());
        vo.setMemberCount(session.getMemberCount());
        if (viewer != null) {
            vo.setUnreadCount(viewer.getUnreadCount() == null ? 0 : viewer.getUnreadCount());
            vo.setLastReadMsgId(viewer.getLastReadMsgId() == null ? 0 : viewer.getLastReadMsgId());
        } else {
            vo.setUnreadCount(0);
            vo.setLastReadMsgId(0L);
        }
        return vo;
    }

    private void applyPeer(ChatSessionVO vo, Integer peerId, User peer) {
        vo.setPeerId(peerId);
        if (peer != null) {
            String name = displayName(peer);
            vo.setName(name);
            vo.setPeerName(name);
            vo.setPeerRole(peer.getUserRole());
        }
    }

    private List<ChatMessageVO> toVOList(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Integer> senderIds = messages.stream().map(ChatMessage::getSenderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, User> users = loadUsers(senderIds);
        Set<Long> fileIds = messages.stream().map(ChatMessage::getFileId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ChatFile> files = loadFiles(fileIds);
        List<ChatMessageVO> result = new ArrayList<>();
        for (ChatMessage message : messages) {
            // 文本消息没有附件，不能用空 Map 查 null key（Map.of 不允许 null）
            ChatFile file = message.getFileId() == null ? null : files.get(message.getFileId());
            result.add(toVO(message, users.get(message.getSenderId()), file));
        }
        return result;
    }

    private ChatMessageVO toVO(ChatMessage message, User sender, ChatFile file) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setSenderId(message.getSenderId());
        vo.setClientMsgId(message.getClientMsgId());
        vo.setMsgType(message.getMsgType());
        vo.setStatus(message.getStatus());
        vo.setCreateTime(message.getCreateTime());
        if (sender != null) {
            vo.setSenderName(displayName(sender));
            vo.setSenderRole(sender.getUserRole());
        }
        // 撤回后不再下发原文与附件信息
        if (STATUS_RECALLED.equals(message.getStatus())) {
            vo.setContent(null);
            return vo;
        }
        vo.setContent(message.getContent());
        if (file != null) {
            vo.setFileId(file.getId());
            vo.setFileName(file.getFileName());
            vo.setFileSize(file.getFileSize());
            vo.setContentType(file.getContentType());
        } else {
            vo.setFileId(message.getFileId());
        }
        return vo;
    }

    private Map<Integer, User> loadUsers(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(new HashSet<>(ids));
        return users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, ChatFile> loadFiles(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<ChatFile> files = fileMapper.selectBatchIds(new HashSet<>(ids));
        return files.stream().collect(Collectors.toMap(ChatFile::getId, Function.identity(), (a, b) -> a));
    }

    private String displayName(User user) {
        if (user == null) {
            return "未知用户";
        }
        return user.getRealName() == null || user.getRealName().isBlank()
                ? user.getUserName() : user.getRealName();
    }

    private String preview(String msgType, String content, ChatFile file) {
        if (MSG_IMAGE.equals(msgType)) {
            return "[图片]";
        }
        if (MSG_FILE.equals(msgType)) {
            return "[文件] " + (file == null ? "" : truncate(file.getFileName(), 40));
        }
        return truncate(content, 60);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private ChatAckVO ack(String clientMsgId, boolean ok, String message, Long messageId) {
        ChatAckVO ack = new ChatAckVO();
        ack.setClientMsgId(clientMsgId);
        ack.setOk(ok);
        ack.setMessage(message);
        ack.setMessageId(messageId);
        return ack;
    }

    /** 事务提交后再广播，避免客户端收到尚未落库的消息 */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
