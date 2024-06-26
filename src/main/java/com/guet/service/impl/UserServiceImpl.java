package com.guet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guet.constants.SystemConstants;
import com.guet.dto.LoginFormDTO;
import com.guet.dto.UserDTO;
import com.guet.entity.User;
import com.guet.mapper.UserMapper;
import com.guet.result.Result;
import com.guet.service.IUserService;
import com.guet.utils.RegexUtil;
import com.guet.utils.UserHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.guet.constants.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate srt;

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/15 17:42:40
     * @Description: 发送验证码
     */
    @Override
    public Result sendCode (String phone, HttpSession session) {
        // 1.验证手机号有效性
        if (RegexUtil.isPhoneInvalid(phone)) {
            // 2.如果不成功，提示手机号有误
            return Result.fail("手机号格式错误！");
        }

        // 3.如果成功，就生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.将code保存到Redis中，3min后过期
        srt.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码，需要使用第三方的发送短信平台，这里就不做了
        log.warn("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/15 17:42:48
     * @Description: 用户登录
     */
    @Override
    public Result login (LoginFormDTO loginForm, HttpSession session) {

        // 1.验证手机号是否有问题，两次请求都需要校验，万一发了验证码又改了手机号怎么办
        String phone = loginForm.getPhone();
        if (RegexUtil.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }

        // 2.从redis取出验证码code，与前端传入的code比较是否相同
        String cacheCode = srt.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        // 判空条件要放在最前面
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 3.不成功返回验证码有误
            return Result.fail("验证码错误！");
        }

        // 4.成功去查询手机号
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            // 5.查询不到，自动进行注册
            user = insertUserWithPhone(phone);
        }

        // 6.将用户信息保存到Redis中，key使用UUID，value使用Hash
        // 6.1 生成key，使用hutool的UUID生成，可以生成没有短横线的UUID
        String tokenKey = LOGIN_USER_KEY + UUID.randomUUID().toString(true);

        // 6.2 构造value
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);

        // 6.3 做一个map，减少与redis服务器的交互次数
        Map<String, Object> map = BeanUtil.beanToMap(userDTO);
        map.replace("id", map.get("id").toString());
        srt.opsForHash().putAll(tokenKey, map);

        // 6.4 设置用户信息的有效期
        srt.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 7.登录成功，删除在Redis中的验证码
        srt.delete(LOGIN_CODE_KEY + phone);

        // 8.将token返回
        return Result.ok(tokenKey);
    }


    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/15 21:41:31
     * @Description: 插入数据，并设置初始的昵称
     */
    private User insertUserWithPhone (String phone) {
        User user = new User();
        user.setPhone(phone)
                .setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10))
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }


    @Override
    public Result me () {
        return Result.ok(UserHolderUtil.getUser());
    }

    @Override
    public Result logout (HttpServletRequest request) {
        String token = request.getHeader("authorization");
        srt.delete(token);
        UserHolderUtil.removeUser();
        return Result.ok();
    }

    @Override
    public Result userSign () {
        // 获取当前用户
        Long userId = UserHolderUtil.getUser().getId();

        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 封装key
        String key = USER_SIGN_KEY + userId + ":" + keySuffix;

        // 得到今天是这个月的第几天，这样才能给对应BitMap索引赋值
        int dayOfMonth = now.getDayOfMonth();

        // 写入Redis，设置BitMap的值
        // 因为所以是从0开始，所以这里要-1；true代表写入1，使用boolean只需要1字节，int要4字节
        srt.opsForValue().setBit(key, dayOfMonth - 1, true);

        // 返回
        return Result.ok();
    }

    @Override
    public Result signCount () {
        // 获取当前用户
        Long userId = UserHolderUtil.getUser().getId();

        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 封装key
        String key = USER_SIGN_KEY + userId + ":" + keySuffix;

        // 得到今天是这个月的第几天，这样才能给对应BitMap索引赋值
        int dayOfMonth = now.getDayOfMonth();

        // 今天是本月的第几天，就去查多少个bit
        // BITFIELD key GET u3 0
        List<Long> results = srt.opsForValue().bitField(key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );

        // 空说明没签到
        if (results.isEmpty()) {
            return Result.ok(0);
        }

        Long num = results.get(0);
        if (num == null) {
            return Result.ok(0);
        }
        // 因为只是get，所以这个集合只有一个值
        // 循环遍历十进制，让这位数字和1做与运算，判断它是否为0
        int count = 0;
        while (true) {
            // 数字和1做and运算
            if ((num & 1) == 0) {
                // 是0就结束
                break;
            } else {
                // 不是0就计数器+1
                count++;
            }
            // 数字右移一位，这样才能判断下一个bit
            num = num >> 1;
        }
        return Result.ok(count);
    }


}
