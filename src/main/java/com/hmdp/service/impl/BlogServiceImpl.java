package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constants.SystemConstants;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.result.Result;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolderUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.hmdp.constants.RedisConstants.BLOG_DETAIL_KEY;
import static com.hmdp.constants.RedisConstants.BLOG_LIKED_KEY;

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

    private void isBlogLiked (Blog blog) {
        Long userId = blog.getUserId();
        String key = BLOG_DETAIL_KEY + blog.getId();
        Double isLike = srt.opsForZSet().score(key, userId.toString());
        blog.setIsLike(isLike != null);
    }

    private void queryUserAndSetBlog (Blog blog) {
        Long userId = blog.getUserId();
        User user = userMapper.selectById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
