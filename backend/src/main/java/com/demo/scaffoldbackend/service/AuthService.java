package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.LoginDTO;
import com.demo.scaffoldbackend.dto.PasswordChangeDTO;
import com.demo.scaffoldbackend.dto.TokenRefreshDTO;
import com.demo.scaffoldbackend.dto.UserDTO;
import com.demo.scaffoldbackend.vo.TokenVO;
import com.demo.scaffoldbackend.vo.UserCheckVO;
import com.demo.scaffoldbackend.vo.UserVO;

public interface AuthService {

    TokenVO login(LoginDTO dto);

    UserVO register(UserDTO dto);

    TokenVO refresh(TokenRefreshDTO dto);

    boolean logout(String token);

    UserVO current(String token);

    boolean changePassword(PasswordChangeDTO dto);

    UserCheckVO check(String userName);
}
