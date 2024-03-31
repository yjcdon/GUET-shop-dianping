package com.GUET.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.GUET.dto.LoginFormDTO;
import com.GUET.result.Result;
import com.GUET.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode (String phone, HttpSession session);

    Result login (LoginFormDTO loginForm, HttpSession session);

    Result me ();

    Result logout (HttpServletRequest request);

    Result userSign ();

    Result signCount ();
}
