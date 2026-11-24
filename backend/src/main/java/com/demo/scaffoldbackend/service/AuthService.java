package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.dto.EmailLoginDTO;
import com.demo.scaffoldbackend.dto.LoginDTO;
import com.demo.scaffoldbackend.dto.PasswordChangeDTO;
import com.demo.scaffoldbackend.dto.SmsLoginDTO;
import com.demo.scaffoldbackend.dto.TokenRefreshDTO;
import com.demo.scaffoldbackend.dto.UserDTO;
import com.demo.scaffoldbackend.vo.LoginStatusVO;
import com.demo.scaffoldbackend.vo.TokenVO;
import com.demo.scaffoldbackend.vo.UserCheckVO;
import com.demo.scaffoldbackend.vo.UserVO;

public interface AuthService {

    TokenVO login(LoginDTO dto);

    /** 手机号 + 短信验证码登录，仅限注册时绑定过该手机号的账号 */
    TokenVO loginBySms(SmsLoginDTO dto);

    /** 邮箱 + 密码登录，仅限注册时绑定过该邮箱的账号 */
    TokenVO loginByEmail(EmailLoginDTO dto);

    UserVO register(UserDTO dto);

    TokenVO refresh(TokenRefreshDTO dto);

    boolean logout(String token);

    UserVO current(String token);

    boolean changePassword(PasswordChangeDTO dto);

    UserCheckVO check(String userName);

    /** 查询登录风控状态（是否锁定、剩余秒数、剩余尝试次数） */
    LoginStatusVO loginStatus(String userName);

    /** 手动解锁（管理员） */
    boolean unlock(String userName);

    // ===== 用户管理（管理员）=====

    PageResult<UserVO> listUsers(int page, int size, String keyword, String role);

    UserVO getUser(Integer id);

    UserVO createUser(UserDTO dto);

    UserVO updateUser(Integer id, UserDTO dto);

    boolean deleteUser(Integer id);
}
