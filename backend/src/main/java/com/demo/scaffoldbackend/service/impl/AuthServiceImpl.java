package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.config.JwtUtil;
import com.demo.scaffoldbackend.dto.LoginDTO;
import com.demo.scaffoldbackend.dto.PasswordChangeDTO;
import com.demo.scaffoldbackend.dto.TokenRefreshDTO;
import com.demo.scaffoldbackend.dto.UserDTO;
import com.demo.scaffoldbackend.entity.User;
import com.demo.scaffoldbackend.mapper.UserMapper;
import com.demo.scaffoldbackend.service.AuthService;
import com.demo.scaffoldbackend.vo.TokenVO;
import com.demo.scaffoldbackend.vo.UserCheckVO;
import com.demo.scaffoldbackend.vo.UserVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Value("${security.login.max-fail:5}")
    private int maxFail;

    @Value("${security.login.lock-minutes:10}")
    private long lockMinutes;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, StringRedisTemplate redis) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redis = redis;
    }

    @Override
    public TokenVO login(LoginDTO dto) {
        if (dto == null || isBlank(dto.getUserName()) || isBlank(dto.getPwd())) {
            throw new BusinessException(400, "账号和密码不能为空");
        }
        String userName = dto.getUserName().trim();
        String lockKey = "login:lock:" + userName;
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new BusinessException(423, "账号已锁定，请稍后再试");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserName, userName));
        if (user == null || !passwordEncoder.matches(dto.getPwd(), user.getPwd())) {
            if (recordFailure(userName, lockKey)) {
                throw new BusinessException(423, "失败次数过多，账号已锁定");
            }
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!"NORMAL".equals(user.getUserStatus())) {
            throw new BusinessException(403, "账号状态异常，请联系管理员");
        }

        redis.delete("login:fail:" + userName);
        redis.delete(lockKey);

        String access = jwtUtil.issueAccess(user.getId(), user.getUserRole());
        String refresh = jwtUtil.issueRefresh(user.getId());
        redis.opsForValue().set("auth:session:" + user.getId(), access, Duration.ofSeconds(jwtUtil.getAccessExpire()));
        redis.opsForValue().set("auth:refresh:" + refresh, String.valueOf(user.getId()), Duration.ofSeconds(jwtUtil.getRefreshExpire()));

        TokenVO vo = new TokenVO();
        vo.setAccessToken(access);
        vo.setRefreshToken(refresh);
        vo.setExpiresIn(jwtUtil.getAccessExpire());
        return vo;
    }

    @Override
    public UserVO register(UserDTO dto) {
        if (dto == null || isBlank(dto.getUserName()) || isBlank(dto.getPwd())) {
            throw new BusinessException(400, "账号和密码不能为空");
        }
        String userName = dto.getUserName().trim();
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserName, userName));
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "账号已存在");
        }
        User user = new User();
        user.setUserName(userName);
        user.setPwd(passwordEncoder.encode(dto.getPwd()));
        user.setUserRole(isBlank(dto.getUserRole()) ? "STUDENT" : dto.getUserRole());
        user.setUserStatus("NORMAL");
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStudentNo(dto.getStudentNo());
        user.setTargetMajor(dto.getTargetMajor());
        user.setSuperviseMode(dto.getSuperviseMode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public TokenVO refresh(TokenRefreshDTO dto) {
        if (dto == null || isBlank(dto.getRefreshToken())) {
            throw new BusinessException(400, "刷新令牌不能为空");
        }
        String refreshToken = dto.getRefreshToken();
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(401, "令牌类型错误");
        }
        if (!Boolean.TRUE.equals(redis.hasKey("auth:refresh:" + refreshToken))) {
            throw new BusinessException(401, "刷新令牌已失效，请重新登录");
        }
        Integer userId = Integer.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 刷新令牌轮换：旧令牌作废
        redis.delete("auth:refresh:" + refreshToken);
        String access = jwtUtil.issueAccess(userId, user.getUserRole());
        String newRefresh = jwtUtil.issueRefresh(userId);
        redis.opsForValue().set("auth:session:" + userId, access, Duration.ofSeconds(jwtUtil.getAccessExpire()));
        redis.opsForValue().set("auth:refresh:" + newRefresh, String.valueOf(userId), Duration.ofSeconds(jwtUtil.getRefreshExpire()));

        TokenVO vo = new TokenVO();
        vo.setAccessToken(access);
        vo.setRefreshToken(newRefresh);
        vo.setExpiresIn(jwtUtil.getAccessExpire());
        return vo;
    }

    @Override
    public boolean logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (isBlank(token)) {
            return true;
        }
        try {
            Integer userId = Integer.valueOf(jwtUtil.parse(token).getSubject());
            redis.delete("auth:session:" + userId);
        } catch (Exception ignored) {
            // 令牌已失效，视为已退出
        }
        return true;
    }

    @Override
    public UserVO current(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (isBlank(token)) {
            throw new BusinessException(401, "未登录");
        }
        Integer userId;
        try {
            userId = Integer.valueOf(jwtUtil.parse(token).getSubject());
        } catch (Exception e) {
            throw new BusinessException(401, "令牌无效或已过期");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public boolean changePassword(PasswordChangeDTO dto) {
        if (dto == null || dto.getUserId() == null || isBlank(dto.getOldPwd()) || isBlank(dto.getNewPwd())) {
            throw new BusinessException(400, "参数不完整");
        }
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPwd(), user.getPwd())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPwd(passwordEncoder.encode(dto.getNewPwd()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        redis.delete("auth:session:" + user.getId());
        return true;
    }

    @Override
    public UserCheckVO check(String userName) {
        if (isBlank(userName)) {
            throw new BusinessException(400, "账号不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserName, userName.trim()));
        UserCheckVO vo = new UserCheckVO();
        vo.setUserName(userName.trim());
        vo.setAvailable(exists == null || exists == 0);
        vo.setReason(vo.getAvailable() ? null : "账号已存在");
        return vo;
    }

    private boolean recordFailure(String userName, String lockKey) {
        String failKey = "login:fail:" + userName;
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, Duration.ofMinutes(lockMinutes));
        }
        if (count != null && count >= maxFail) {
            redis.opsForValue().set(lockKey, "locked", Duration.ofMinutes(lockMinutes));
            redis.delete(failKey);
            return true;
        }
        return false;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
