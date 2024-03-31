package com.GUET.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.GUET.constants.SystemConstants;
import com.GUET.dto.ScrollResultDTO;
import com.GUET.dto.UserDTO;
import com.GUET.entity.Blog;
import com.GUET.entity.Follow;
import com.GUET.entity.User;
import com.GUET.mapper.BlogMapper;
import com.GUET.mapper.FollowMapper;
import com.GUET.mapper.UserMapper;
import com.GUET.result.Result;
import com.GUET.service.IBlogService;
import com.GUET.utils.UserHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.GUET.constants.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FollowMapper followMapper;

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/1 14:26:48
     * @Description: 点击笔记，查看详情
     */
    @Override
    public Result queryBlogById (Long id) throws Exception {
        Blog blog = blogMapper.selectById(id);
        if (blog == null) {
            return Result.fail("blog不存在");
        }
        isBlogLiked(blog);
        return Result.ok(blog);
    }


    @Override
    public Result queryHotBlog (@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据用户查询
        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getLiked);
        Page<Blog> page = blogMapper.selectPage(new Page<>(current, SystemConstants.MAX_PAGE_SIZE), queryWrapper);
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryUserAndSetBlog(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/1 11:52:10
     * @Description: 点赞blog，按照点赞时间进行排序
     */
    @Override
    public Result likeBlog (Long id) {
        // 获取登录用户
        Long userId = UserHolderUtil.getUser().getId();

        // 判断是否点赞
        String key = BLOG_LIKED_KEY + id;// 这个ID是笔记的ID
        Double score = srt.opsForZSet().score(key, userId.toString());

        // 没点赞，就让点赞数+1，把userId放入Redis的Set中
        if (score == null) {
            // 为了减少一次数据库交互
            boolean isSuccess = update()
                    .setSql("liked = liked + 1").eq("id", id)
                    .update();
            if (isSuccess) {
                srt.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        // 点赞了，又点了一次，则点赞数-1，把userId移出Redis的Set中
        else {
            boolean isSuccess = update()
                    .setSql("liked = liked - 1").eq("id", id)
                    .update();
            if (isSuccess) {
                srt.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public List<UserDTO> queryTop5 (Long id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> userIds = srt.opsForZSet().range(key, 0, 4);
        if (!userIds.isEmpty()) {
            return blogMapper.queryUserDTOByIds(userIds);
        }
        return null;
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/2 17:30:54
     * @Params:
     * @Return:
     * @Description: 查询并设置该blog是否被当前用户点过赞
     */

    private void isBlogLiked (Blog blog) {
        Long userId = blog.getUserId();
        String key = BLOG_DETAIL_KEY + blog.getId();
        Double isLike = srt.opsForZSet().score(key, userId.toString());
        blog.setIsLike(isLike != null);
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/2 17:30:10
     * @Params:
     * @Return:
     * @Description: 设置该blog的发布者的name和投降
     */
    private void queryUserAndSetBlog (Blog blog) {
        Long userId = blog.getUserId();
        User user = userMapper.selectById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result saveBlogAndPush (Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolderUtil.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        int isSuccess = blogMapper.insert(blog);

        if (isSuccess <= 0) {
            return Result.fail("发送失败！");
        }

        // 查询当前用户的粉丝
        Long userId = user.getId();
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId, userId);
        List<Follow> follows = followMapper.selectList(queryWrapper);// 当前用户的粉丝列表
        if (!follows.isEmpty()) {
            for (Follow follow : follows) {
                // 获取粉丝id
                Long followUserId = follow.getUserId();
                // 推送给每个粉丝，每个粉丝都有一个收件箱，使用zset实现收件箱
                String key = FEED_KEY + followUserId;
                srt.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
            }
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow (Long maxTime, Integer offset) {
        // 获取当前用户
        Long userId = UserHolderUtil.getUser().getId();

        // 查询收件箱
        String key = FEED_KEY + userId;

        // 滚动分页查询：ZREVRANGEBYSCORE key Max Min LIMIT offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples = srt.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, maxTime, offset, 3);
        if (typedTuples.isEmpty()) {
            return Result.ok();
        }

        // 解析数据：有blogId，minTime（时间戳），offset
        List<Long> blogIds = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> type : typedTuples) {
            // 获取blogID
            blogIds.add(Long.valueOf(type.getValue()));
            // 获取这条数据对应的时间戳，这是栈吗，集合最后一个保证时间戳是最小的？
            long time = type.getScore().longValue();

            // 找出offset，就是zset中重复score的元素个数
            // 比如是5 4 4 2 2，第一次是5，minTime就是5；第二次是4，minTime是4；第三次是4，os++，以此类推
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }

        // 根据blogIds查询blog
        String idStr = StrUtil.join(",", blogIds);
        List<Blog> blogList = query()
                .in("id", blogIds).last("ORDER BY FIELD(id," + idStr + ")")
                .list();
        for (Blog blog : blogList) {
            // 查询和设置每条blog的相关信息
            queryUserAndSetBlog(blog);
            isBlogLiked(blog);
        }

        // 封装结果并返回
        ScrollResultDTO res = new ScrollResultDTO();
        res.setList(blogList);
        res.setOffset(os);
        res.setMinTime(minTime);

        return Result.ok(res);
    }
}
