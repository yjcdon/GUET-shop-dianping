package com.guet.interceptor;

import com.guet.utils.UserHolderUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: 梁雨佳
 * @Date: 2024/2/16 11:04:58
 * @Description: 登录的第二道拦截器
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否存在用户
        if (UserHolderUtil.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        // 放行
        return true;
    }
}
