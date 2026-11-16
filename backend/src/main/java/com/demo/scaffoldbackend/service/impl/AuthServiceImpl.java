package com.demo.scaffoldbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.config.JwtUtil;
import com.demo.scaffoldbackend.dto.EmailLoginDTO;
import com.demo.scaffoldbackend.dto.LoginDTO;
import com.demo.scaffoldbackend.dto.PasswordChangeDTO;
import com.demo.scaffoldbackend.dto.TokenRefreshDTO;
import com.demo.scaffoldbackend.dto.UserDTO;
import com.demo.scaffoldbackend.entity.User;
import com.demo.scaffoldbackend.dto.SmsLoginDTO;
import com.demo.scaffoldbackend.mapper.UserMapper;
import com.demo.scaffoldbackend.service.AuthService;
import com.demo.scaffoldbackend.service.CaptchaService;
import com.demo.scaffoldbackend.service.EmailCodeService;
import com.demo.scaffoldbackend.service.SmsService;
import com.demo.scaffoldbackend.vo.LoginStatusVO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final EmailCodeService emailCodeService;

    @Value("${security.login.max-fail:5}")
    private int maxFail;

    @Value("${security.login.lock-minutes:3}")
    private long lockMinutes;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, StringRedisTemplate redis,
                           CaptchaService captchaService, SmsService smsService,
                           EmailCodeService emailCodeService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redis = redis;
        this.captchaService = captchaService;
        this.smsService = smsService;
        this.emailCodeService = emailCodeService;
    }

    @Override
    public TokenVO login(LoginDTO dto) {
        if (dto == null || isBlank(dto.getUserName()) || isBlank(dto.getPwd())) {
            throw new BusinessException(400, "账号和密码不能为空");
        }
        String userName = dto.getUserName().trim();
        String lockKey = "login:lock:" + userName;
        long lockRemain = remainSeconds(lockKey);
        if (lockRemain > 0) {
            throw new BusinessException(423, "账号已锁定，请 " + formatDuration(lockRemain) + " 后重试");
        }

        // 密码登录不校验图形验证码：由失败次数锁定 + 接口限流防暴力破解，便于演示连续失败锁定
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserName, userName));
        if (user == null || !passwordEncoder.matches(dto.getPwd(), user.getPwd())) {
            int remaining = recordFailure(userName, lockKey);
            if (remaining <= 0) {
                throw new BusinessException(423, "连续 " + maxFail + " 次失败，账号已锁定 " + lockMinutes + " 分钟");
            }
            throw new BusinessException(401, "账号或密码错误，还可尝试 " + remaining + " 次");
        }
        if (!"NORMAL".equals(user.getUserStatus())) {
            throw new BusinessException(403, "账号状态异常，请联系管理员");
        }

        redis.delete("login:fail:" + userName);
        redis.delete(lockKey);
        return issueTokens(user);
    }

    @Override
    public TokenVO loginBySms(SmsLoginDTO dto) {
        if (dto == null || isBlank(dto.getPhone()) || isBlank(dto.getSmsCode())) {
            throw new BusinessException(400, "手机号和短信验证码不能为空");
        }
        String phone = dto.getPhone().trim();
        smsService.verifyLoginCode(phone, dto.getSmsCode().trim());

        // 手机号登录只对已注册且绑定了该手机号的账号开放
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (users == null || users.isEmpty()) {
            throw new BusinessException(400, "该手机号未注册，请先注册并绑定手机号");
        }
        User user = users.get(0);
        if (!"NORMAL".equals(user.getUserStatus())) {
            throw new BusinessException(403, "账号状态异常，请联系管理员");
        }
        return issueTokens(user);
    }

    @Override
    public TokenVO loginByEmail(EmailLoginDTO dto) {
        if (dto == null || isBlank(dto.getEmail()) || isBlank(dto.getEmailCode())) {
            throw new BusinessException(400, "邮箱和验证码不能为空");
        }
        String email = dto.getEmail().trim().toLowerCase();

        // 校验邮箱验证码（一次性消费，5 次错误作废）
        emailCodeService.verifyLoginCode(email, dto.getEmailCode().trim());

        // 邮箱登录只对注册时绑定过该邮箱的账号开放，不会自动建号
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException(400, "该邮箱未注册，请先注册并绑定邮箱");
        }
        if (!"NORMAL".equals(user.getUserStatus())) {
            throw new BusinessException(403, "账号状态异常，请联系管理员");
        }
        return issueTokens(user);
    }

    /** 统一签发双令牌并写入 Redis 会话 */
    private TokenVO issueTokens(User user) {
        String access = jwtUtil.issueAccess(user.getId(), user.getUserRole());
        String refresh = jwtUtil.issueRefresh(user.getId());
        saveTokens(user.getId(), access, refresh);
        recordDashboardLogin(user);

        TokenVO vo = new TokenVO();
        vo.setAccessToken(access);
        vo.setRefreshToken(refresh);
        vo.setExpiresIn(jwtUtil.getAccessExpire());
        return vo;
    }

    /**
     * 记录真实登录产生的 Redis 实时指标：当天活跃用户与动态列表。
     * 同一用户当天重复登录由 Set 自动去重。
     */
    private void recordDashboardLogin(User user) {
        String activeKey = "postgraduate:active:" + LocalDate.now();
        redis.opsForSet().add(activeKey, String.valueOf(user.getId()));
        redis.expire(activeKey, Duration.ofDays(8));

        String activityKey = "postgraduate:activity";
        String activity = LocalDateTime.now() + " · 用户「" + user.getUserName() + "」登录成功";
        redis.opsForList().leftPush(activityKey, activity);
        redis.opsForList().trim(activityKey, 0, 99);
        redis.expire(activityKey, Duration.ofDays(7));
    }

    @Override
    public UserVO register(UserDTO dto) {
        if (dto == null || isBlank(dto.getUserName()) || isBlank(dto.getPwd())) {
            throw new BusinessException(400, "账号和密码不能为空");
        }
        String userName = dto.getUserName().trim();
        if (dto.getPwd().length() < 6) {
            throw new BusinessException(400, "密码长度不能少于 6 位");
        }
        // 手机号与邮箱均为可选绑定：可只绑一个，也可都暂不绑定；绑定后才会开启对应登录方式
        String phone = normalize(dto.getPhone());
        String email = normalizeEmail(dto.getEmail());
        if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的 11 位手机号");
        }
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException(400, "请输入正确的邮箱地址");
        }
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserName, userName));
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "账号已存在");
        }
        if (!phone.isEmpty()) {
            Long phoneExists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (phoneExists != null && phoneExists > 0) {
                throw new BusinessException(409, "该手机号已被注册");
            }
        }
        if (!email.isEmpty()) {
            Long emailExists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (emailExists != null && emailExists > 0) {
                throw new BusinessException(409, "该邮箱已被注册");
            }
        }
        User user = new User();
        user.setUserName(userName);
        user.setPwd(passwordEncoder.encode(dto.getPwd()));
        // 公开注册固定为学生，角色提升只能由管理员在后台操作
        user.setUserRole("STUDENT");
        user.setUserStatus("NORMAL");
        user.setRealName(dto.getRealName());
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setEmail(email.isEmpty() ? null : email);
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
        saveTokens(userId, access, newRefresh);

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
            invalidateSessions(userId);
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
        invalidateSessions(user.getId());
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

    @Override
    public LoginStatusVO loginStatus(String userName) {
        if (isBlank(userName)) {
            throw new BusinessException(400, "账号不能为空");
        }
        String name = userName.trim();
        LoginStatusVO vo = new LoginStatusVO();
        vo.setUserName(name);
        vo.setMaxFail(maxFail);

        long lockRemain = remainSeconds("login:lock:" + name);
        boolean locked = lockRemain > 0;
        vo.setLocked(locked);
        vo.setRemainSeconds(locked ? lockRemain : 0);
        if (locked) {
            vo.setRemainingAttempts(0);
        } else {
            int fail = parseCount(redis.opsForValue().get("login:fail:" + name));
            vo.setRemainingAttempts(Math.max(0, maxFail - fail));
        }
        return vo;
    }

    @Override
    public boolean unlock(String userName) {
        if (isBlank(userName)) {
            throw new BusinessException(400, "账号不能为空");
        }
        String name = userName.trim();
        redis.delete("login:lock:" + name);
        redis.delete("login:fail:" + name);
        return true;
    }

    @Override
    public PageResult<UserVO> listUsers(int page, int size, String keyword, String role) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (!isBlank(keyword)) {
            String kw = keyword.trim();
            query.and(w -> w.like(User::getUserName, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getPhone, kw)
                    .or().like(User::getEmail, kw));
        }
        if (!isBlank(role)) {
            query.eq(User::getUserRole, role.trim().toUpperCase());
        }
        query.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<UserVO> records = new ArrayList<>();
        for (User item : result.getRecords()) {
            records.add(toVO(item));
        }
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Override
    public UserVO getUser(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO createUser(UserDTO dto) {
        if (dto == null || isBlank(dto.getUserName()) || isBlank(dto.getPwd())) {
            throw new BusinessException(400, "账号和密码不能为空");
        }
        String userName = dto.getUserName().trim();
        if (dto.getPwd().length() < 6) {
            throw new BusinessException(400, "密码长度不能少于 6 位");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserName, userName));
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "账号已存在");
        }
        String phone = normalize(dto.getPhone());
        String email = normalizeEmail(dto.getEmail());
        checkContact(phone, email, null);

        User user = new User();
        user.setUserName(userName);
        user.setPwd(passwordEncoder.encode(dto.getPwd()));
        // 教师 / 管理员账号只能由管理员在这里创建，公开注册永远是学生
        user.setUserRole(checkRole(dto.getUserRole(), "STUDENT"));
        user.setUserStatus("DISABLED".equals(dto.getUserStatus()) ? "DISABLED" : "NORMAL");
        user.setRealName(dto.getRealName());
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setEmail(email.isEmpty() ? null : email);
        user.setStudentNo(dto.getStudentNo());
        user.setTargetMajor(dto.getTargetMajor());
        user.setSuperviseMode(dto.getSuperviseMode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserVO updateUser(Integer id, UserDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (dto == null) {
            return toVO(user);
        }
        boolean sessionChanged = false;
        if (!isBlank(dto.getUserName()) && !dto.getUserName().trim().equals(user.getUserName())) {
            String newName = dto.getUserName().trim();
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUserName, newName)
                    .ne(User::getId, id));
            if (exists != null && exists > 0) {
                throw new BusinessException(409, "账号已存在");
            }
            user.setUserName(newName);
        }
        if (!isBlank(dto.getPwd())) {
            if (dto.getPwd().length() < 6) {
                throw new BusinessException(400, "密码长度不能少于 6 位");
            }
            user.setPwd(passwordEncoder.encode(dto.getPwd()));
            sessionChanged = true;
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null) {
            String phone = normalize(dto.getPhone());
            String email = normalizeEmail(dto.getEmail() == null ? user.getEmail() : dto.getEmail());
            checkContact(phone, email, id);
            user.setPhone(phone.isEmpty() ? null : phone);
            user.setEmail(email.isEmpty() ? null : email);
        } else if (dto.getEmail() != null) {
            String email = normalizeEmail(dto.getEmail());
            checkContact(user.getPhone(), email, id);
            user.setEmail(email.isEmpty() ? null : email);
        }
        if (!isBlank(dto.getUserRole())) {
            String role = checkRole(dto.getUserRole(), user.getUserRole());
            if (!role.equals(user.getUserRole())) {
                user.setUserRole(role);
                sessionChanged = true;
            }
        }
        if (!isBlank(dto.getUserStatus())) {
            String status = "DISABLED".equals(dto.getUserStatus()) ? "DISABLED" : "NORMAL";
            if (!status.equals(user.getUserStatus())) {
                user.setUserStatus(status);
                sessionChanged = true;
            }
        }
        if (dto.getStudentNo() != null) {
            user.setStudentNo(dto.getStudentNo());
        }
        if (dto.getTargetMajor() != null) {
            user.setTargetMajor(dto.getTargetMajor());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        if (sessionChanged) {
            // 角色或状态变化后旧令牌立即失效，重新登录才会拿到新的角色
            invalidateSessions(id);
        }
        return toVO(user);
    }

    @Override
    public boolean deleteUser(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("ADMIN".equals(user.getUserRole())) {
            Long admins = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserRole, "ADMIN"));
            if (admins != null && admins <= 1) {
                throw new BusinessException(400, "系统至少需要保留一个管理员账号");
            }
        }
        userMapper.deleteById(id);
        invalidateSessions(id);
        return true;
    }

    /** 校验手机号 / 邮箱格式与唯一性（excludeId 用于更新时排除自身） */
    private void checkContact(String phone, String email, Integer excludeId) {
        if (!phone.isEmpty()) {
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(400, "请输入正确的 11 位手机号");
            }
            LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getPhone, phone);
            if (excludeId != null) {
                query.ne(User::getId, excludeId);
            }
            Long count = userMapper.selectCount(query);
            if (count != null && count > 0) {
                throw new BusinessException(409, "该手机号已被使用");
            }
        }
        if (!email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BusinessException(400, "请输入正确的邮箱地址");
            }
            LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getEmail, email);
            if (excludeId != null) {
                query.ne(User::getId, excludeId);
            }
            Long count = userMapper.selectCount(query);
            if (count != null && count > 0) {
                throw new BusinessException(409, "该邮箱已被使用");
            }
        }
    }

    private String checkRole(String role, String fallback) {
        if (isBlank(role)) {
            return fallback;
        }
        String value = role.trim().toUpperCase();
        if (!Set.of("STUDENT", "TEACHER", "ADMIN").contains(value)) {
            throw new BusinessException(400, "角色只能是 STUDENT / TEACHER / ADMIN");
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }

    /** 保存会话：同一用户仅保留最新访问令牌与刷新令牌 */
    private void saveTokens(Integer userId, String access, String refresh) {
        redis.opsForValue().set("auth:session:" + userId, access, Duration.ofSeconds(jwtUtil.getAccessExpire()));
        redis.opsForValue().set("auth:refresh:" + refresh, String.valueOf(userId), Duration.ofSeconds(jwtUtil.getRefreshExpire()));
        redis.opsForValue().set("auth:refresh:user:" + userId, refresh, Duration.ofSeconds(jwtUtil.getRefreshExpire()));
    }

    /** 注销会话：访问令牌立即失效，同时作废该用户的刷新令牌 */
    private void invalidateSessions(Integer userId) {
        redis.delete("auth:session:" + userId);
        String refresh = redis.opsForValue().get("auth:refresh:user:" + userId);
        if (!isBlank(refresh)) {
            redis.delete("auth:refresh:" + refresh);
        }
        redis.delete("auth:refresh:user:" + userId);
    }

    /**
     * 记录一次登录失败
     *
     * @return 剩余可尝试次数；返回 0 表示已触发锁定
     */
    private int recordFailure(String userName, String lockKey) {
        String failKey = "login:fail:" + userName;
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, Duration.ofMinutes(lockMinutes));
        }
        if (count != null && count >= maxFail) {
            redis.opsForValue().set(lockKey, "locked", Duration.ofMinutes(lockMinutes));
            redis.delete(failKey);
            return 0;
        }
        return (int) (maxFail - (count == null ? 0 : count));
    }

    private long remainSeconds(String key) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        return ttl == null ? 0 : Math.max(0, ttl);
    }

    private int parseCount(String value) {
        if (isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDuration(long seconds) {
        if (seconds >= 60) {
            return (seconds + 59) / 60 + " 分钟";
        }
        return seconds + " 秒";
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
