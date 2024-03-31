package com.guet.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.guet.dto.UserDTO;
import com.guet.constants.RedisConstants;
import com.guet.utils.UserHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 梁雨佳
 * @Date: 2024/2/16 18:10:20
 * @Description: 登录的第一道拦截器
 */
@Component
public class RefreshExpireInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate srt;

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的Token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 2.基于Token，从Redis中获取用户信息
        Map<Object, Object> userMap = srt.opsForHash().entries(token);

        // entries会判断返回值是否为null，是null则返回空map
        if (userMap.isEmpty()) {
            return true;
        }

        // 3.将map转为userDTO
        UserDTO userDTO = new UserDTO();
        BeanUtil.fillBeanWithMap(userMap, userDTO, false);

        // 4.存在则保存到ThreadLocal
        UserHolderUtil.saveUser(userDTO);

        // 5.刷新有效期
        srt.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion (HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolderUtil.removeUser();
    }
}
