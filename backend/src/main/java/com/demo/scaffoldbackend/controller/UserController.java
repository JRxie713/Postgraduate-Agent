package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.LoginDTO;
import com.demo.scaffoldbackend.dto.PasswordChangeDTO;
import com.demo.scaffoldbackend.dto.TokenRefreshDTO;
import com.demo.scaffoldbackend.dto.UserDTO;
import com.demo.scaffoldbackend.dto.UserProfileDTO;
import com.demo.scaffoldbackend.vo.TokenVO;
import com.demo.scaffoldbackend.vo.UserCheckVO;
import com.demo.scaffoldbackend.vo.UserProfileVO;
import com.demo.scaffoldbackend.vo.UserVO;
import org.springframework.web.bind.annotation.*;

/**
 * 用户与认证模块
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    // ===== 认证 =====

    /** 登录 */
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody LoginDTO loginDTO) {
        return null;
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody UserDTO userDTO) {
        return null;
    }

    /** 刷新令牌 */
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestBody TokenRefreshDTO tokenRefreshDTO) {
        return null;
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        return null;
    }

    /** 获取当前登录用户 */
    @GetMapping("/current")
    public Result<UserVO> current(@RequestHeader(value = "Authorization", required = false) String token) {
        return null;
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Boolean> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO) {
        return null;
    }

    /** 校验账号可用性 */
    @GetMapping("/check")
    public Result<UserCheckVO> check(@RequestParam String userName) {
        return null;
    }

    // ===== 用户管理 =====

    /** 分页查询用户列表 */
    @GetMapping
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询用户 */
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增用户 */
    @PostMapping
    public Result<UserVO> create(@RequestBody UserDTO userDTO) {
        return null;
    }

    /** 更新用户 */
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Integer id, @RequestBody UserDTO userDTO) {
        return null;
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
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
