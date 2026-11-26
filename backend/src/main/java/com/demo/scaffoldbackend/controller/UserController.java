package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.annotation.RateLimit;
import com.demo.scaffoldbackend.common.exception.BusinessException;
import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.EmailCodeDTO;
import com.demo.scaffoldbackend.dto.EmailLoginDTO;
import com.demo.scaffoldbackend.dto.LoginDTO;
import com.demo.scaffoldbackend.dto.PasswordChangeDTO;
import com.demo.scaffoldbackend.dto.SmsCodeDTO;
import com.demo.scaffoldbackend.dto.SmsLoginDTO;
import com.demo.scaffoldbackend.dto.TokenRefreshDTO;
import com.demo.scaffoldbackend.dto.UserDTO;
import com.demo.scaffoldbackend.dto.UserProfileDTO;
import com.demo.scaffoldbackend.service.AuthService;
import com.demo.scaffoldbackend.service.CaptchaService;
import com.demo.scaffoldbackend.service.EmailCodeService;
import com.demo.scaffoldbackend.service.SmsService;
import com.demo.scaffoldbackend.vo.CaptchaVO;
import com.demo.scaffoldbackend.vo.EmailCodeVO;
import com.demo.scaffoldbackend.vo.LoginStatusVO;
import com.demo.scaffoldbackend.vo.SmsCodeVO;
import com.demo.scaffoldbackend.vo.TokenVO;
import com.demo.scaffoldbackend.vo.UserCheckVO;
import com.demo.scaffoldbackend.vo.UserProfileVO;
import com.demo.scaffoldbackend.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 用户与认证模块
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final EmailCodeService emailCodeService;

    public UserController(AuthService authService, CaptchaService captchaService,
                          SmsService smsService, EmailCodeService emailCodeService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.smsService = smsService;
        this.emailCodeService = emailCodeService;
    }

    // ===== 认证 =====

    /** 获取图形验证码 */
    @RateLimit(limit = 30, window = 60)
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.generate());
    }

    /** 登录 */
    @RateLimit(limit = 10, window = 60)
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    /** 发送短信验证码（发送前校验图形验证码） */
    @RateLimit(limit = 5, window = 60)
    @PostMapping("/sms-code")
    public Result<SmsCodeVO> smsCode(@Valid @RequestBody SmsCodeDTO smsCodeDTO) {
        return Result.success(smsService.sendLoginCode(smsCodeDTO));
    }

    /** 手机号 + 短信验证码登录 */
    @RateLimit(limit = 10, window = 60)
    @PostMapping("/login/sms")
    public Result<TokenVO> loginBySms(@Valid @RequestBody SmsLoginDTO smsLoginDTO) {
        return Result.success(authService.loginBySms(smsLoginDTO));
    }

    /** 发送邮箱验证码（发送前校验图形验证码，真实投递到邮箱） */
    @RateLimit(limit = 5, window = 60)
    @PostMapping("/email-code")
    public Result<EmailCodeVO> emailCode(@Valid @RequestBody EmailCodeDTO emailCodeDTO) {
        return Result.success(emailCodeService.sendLoginCode(emailCodeDTO));
    }

    /** 邮箱 + 邮箱验证码登录 */
    @RateLimit(limit = 10, window = 60)
    @PostMapping("/login/email")
    public Result<TokenVO> loginByEmail(@Valid @RequestBody EmailLoginDTO emailLoginDTO) {
        return Result.success(authService.loginByEmail(emailLoginDTO));
    }

    /** 查询登录风控状态：是否锁定、解锁倒计时、剩余尝试次数 */
    @RateLimit(limit = 60, window = 60)
    @GetMapping("/login/status")
    public Result<LoginStatusVO> loginStatus(@RequestParam String userName) {
        return Result.success(authService.loginStatus(userName));
    }

    /** 手动解锁（需要 user:unlock 权限） */
    @PreAuthorize("hasAuthority('user:unlock')")
    @PostMapping("/unlock/{userName}")
    public Result<Boolean> unlock(@PathVariable String userName) {
        return Result.success(authService.unlock(userName));
    }

    /** 注册 */
    @RateLimit(limit = 10, window = 60)
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody UserDTO userDTO) {
        return Result.success(authService.register(userDTO));
    }

    /** 刷新令牌 */
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestBody TokenRefreshDTO tokenRefreshDTO) {
        return Result.success(authService.refresh(tokenRefreshDTO));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        return Result.success(authService.logout(token));
    }

    /** 获取当前登录用户 */
    @GetMapping("/current")
    public Result<UserVO> current(@RequestHeader(value = "Authorization", required = false) String token) {
        return Result.success(authService.current(token));
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Boolean> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO) {
        return Result.success(authService.changePassword(passwordChangeDTO));
    }

    /** 校验账号可用性 */
    @GetMapping("/check")
    public Result<UserCheckVO> check(@RequestParam String userName) {
        return Result.success(authService.check(userName));
    }

    // ===== 用户管理（仅管理员）=====

    /** 分页查询用户列表（可按账号/姓名/手机/邮箱搜索、按角色过滤） */
    @PreAuthorize("hasAuthority('user:view')")
    @GetMapping
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role) {
        return Result.success(authService.listUsers(page, size, keyword, role));
    }

    /** 根据ID查询用户 */
    @PreAuthorize("hasAuthority('user:view')")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Integer id) {
        return Result.success(authService.getUser(id));
    }

    /** 新增用户：管理员可创建学生 / 教师 / 管理员账号 */
    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public Result<UserVO> create(@RequestBody UserDTO userDTO) {
        return Result.success(authService.createUser(userDTO));
    }

    /** 更新用户：角色、状态、联系方式、密码重置 */
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Integer id, @RequestBody UserDTO userDTO) {
        return Result.success(authService.updateUser(id, userDTO));
    }

    /** 删除用户 */
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id, Authentication authentication) {
        if (authentication != null && String.valueOf(id).equals(authentication.getName())) {
            throw new BusinessException(400, "不能删除当前登录的账号");
        }
        return Result.success(authService.deleteUser(id));
    }

    // ===== 备考画像 =====

    /** 根据用户ID查询备考画像 */
    @GetMapping("/profile/user/{userId}")
    public Result<UserProfileVO> getProfileByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 根据ID查询备考画像 */
    @GetMapping("/profile/{id}")
    public Result<UserProfileVO> getProfileById(@PathVariable Integer id) {
        return null;
    }

    /** 新增备考画像 */
    @PostMapping("/profile")
    public Result<UserProfileVO> createProfile(@RequestBody UserProfileDTO profileDTO) {
        return null;
    }

    /** 更新备考画像 */
    @PutMapping("/profile/{id}")
    public Result<UserProfileVO> updateProfile(@PathVariable Integer id, @RequestBody UserProfileDTO profileDTO) {
        return null;
    }

    /** 删除备考画像 */
    @DeleteMapping("/profile/{id}")
    public Result<Boolean> deleteProfile(@PathVariable Integer id) {
        return null;
    }
}
