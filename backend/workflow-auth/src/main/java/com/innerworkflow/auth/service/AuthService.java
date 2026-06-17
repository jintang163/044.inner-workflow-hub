package com.innerworkflow.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.innerworkflow.auth.dto.LoginDTO;
import com.innerworkflow.auth.dto.RegisterDTO;
import com.innerworkflow.auth.vo.LoginVO;
import com.innerworkflow.auth.vo.UserInfoVO;

public interface AuthService extends IService<Object> {

    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    void logout();

    UserInfoVO getUserInfo();
}
