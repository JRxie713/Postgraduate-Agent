package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.ChatGroupSessionDTO;
import com.demo.scaffoldbackend.dto.ChatSendDTO;
import com.demo.scaffoldbackend.vo.ChatContactVO;
import com.demo.scaffoldbackend.vo.ChatMemberVO;
import com.demo.scaffoldbackend.vo.ChatMessageVO;
import com.demo.scaffoldbackend.vo.ChatReadVO;
import com.demo.scaffoldbackend.vo.ChatSessionVO;

import java.util.List;

/**
 * 聊天核心服务
 */
public interface ChatService {

    List<ChatSessionVO> listSessions(Integer userId);

    ChatSessionVO createPrivateSession(Integer userId, Integer peerId);

    ChatSessionVO createGroupSession(Integer userId, ChatGroupSessionDTO dto);

    ChatSessionVO getSession(Integer userId, Long sessionId);

    List<ChatMemberVO> listMembers(Integer userId, Long sessionId);

    void addMembers(Integer userId, Long sessionId, List<Integer> memberIds);

    List<ChatMessageVO> history(Integer userId, Long sessionId, Long afterId, Long beforeId, Integer size);

    ChatMessageVO send(Integer userId, ChatSendDTO dto);

    ChatReadVO markRead(Integer userId, Long sessionId, Long lastReadMsgId);

    ChatMessageVO recall(Integer userId, Long messageId);

    void typing(Integer userId, Long sessionId);

    boolean isMember(Long sessionId, Integer userId);

    List<ChatContactVO> contacts(Integer userId);
}
