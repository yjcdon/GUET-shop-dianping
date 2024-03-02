package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.result.Result;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.constants.RedisConstants.FOLLOWS_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private StringRedisTemplate srt;

    @Override
    public Result followOrNot (Long id, Boolean isFollow) {
        // 获取当前用户
        Long userId = UserHolderUtil.getUser().getId();
        String key = FOLLOWS_KEY + userId;

        // 如果传入true，则关注该博主，并插入表follow
        if (isFollow) {
            Follow follow = new Follow();
            follow.setUserId(userId)
                    .setFollowUserId(id);
            int isSuccess = followMapper.insert(follow);
            // 然后放入Redis
            if (isSuccess > 0) {
                // 把关注的用户ID放入Set
                srt.opsForSet().add(key, id.toString());
            }
            return Result.ok();
        }

        // 传入false，取消关注，并删除表中的数据
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId, id)
                .eq(Follow::getUserId, userId);
        int isSuccess = followMapper.delete(queryWrapper);
        if (isSuccess > 0) {
            // 删除Redis的Set中是博主ID
            srt.opsForSet().remove(key, id.toString());
        }
        return Result.ok();
    }

    @Override
    public Result queryIsFollow (Long id) {
        // 根据传入的博主id，和当前用户ID进行查询follow表是否有该条数据
        Long userId = UserHolderUtil.getUser().getId();
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId, id)
                .eq(Follow::getUserId, userId);
        Long count = followMapper.selectCount(queryWrapper);

        return Result.ok(count > 0);
    }

    @Override
    public Result queryCommon (Long id) {
        Long userId = UserHolderUtil.getUser().getId();

        // 从Redis中获取的逻辑
        String userKey = FOLLOWS_KEY + userId;
        String blogerKey = FOLLOWS_KEY + id;
        List<UserDTO> intersect = getIntersect(userKey, blogerKey);
        if (!intersect.isEmpty()) {
            return Result.ok(intersect);
        }

        // 如果Redis中找不到，可能是没缓存，来数据库查；如果有一个List是空就直接返回空列表
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();

        // 查当前用户的关注列表，将列表中的followUserId放到Redis
        queryWrapper.eq(Follow::getUserId, userId);
        List<Follow> userFollows = followMapper.selectList(queryWrapper);
        if (!userFollows.isEmpty()) {
            for (Follow userFollow : userFollows) {
                srt.opsForSet().add(userKey, userFollow.getFollowUserId().toString());
            }
        } else {
            return Result.ok(Collections.emptyList());
        }

        // 查当前看的博主的关注列表，并放入Redis中
        queryWrapper.clear();
        queryWrapper.eq(Follow::getUserId, id);
        List<Follow> blogerFollows = followMapper.selectList(queryWrapper);
        if (!blogerFollows.isEmpty()) {
            for (Follow blogerFollow : blogerFollows) {
                srt.opsForSet().add(blogerKey, blogerFollow.getFollowUserId().toString());
            }
        } else {
            return Result.ok(Collections.emptyList());
        }

        // 求两个Set的交集
        return Result.ok(getIntersect(userKey, blogerKey));
    }

    private List<UserDTO> getIntersect (String userKey, String blogerKey) {
        Set<String> intersect = srt.opsForSet().intersect(userKey, blogerKey);
        if (!intersect.isEmpty()) {
            // 解析出id
            List<Long> commonIds = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
            List<UserDTO> res = userService.listByIds(commonIds).stream()
                    .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                    .collect(Collectors.toList());
            return res;
        }
        return Collections.emptyList();
    }
}
