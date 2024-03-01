package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.constants.SystemConstants;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.result.Result;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolderUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;

    @PostMapping
    public Result saveBlog (@RequestBody Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolderUtil.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    @PutMapping("/like/{id}")
    public Result likeBlog (@PathVariable("id") Long id) {
        // 修改点赞数量
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog (@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolderUtil.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog (@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/1 10:39:37
     * @Description: 点击笔记，进入详情页显示信息，传入笔记ID即可
     */
    @GetMapping("/{id}")
    public Result queryBlogById (@PathVariable("id") Long id) throws Exception {
        return blogService.queryBlogById(id);
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/1 16:36:55
     * @Params: blog的ID
     * @Return: 最早的5人的信息List
     * @Description: 查询最早的5人的信息List
     */
    @GetMapping("/likes/{id}")
    public Result queryTop5 (@PathVariable Long id) {
        return Result.ok(blogService.queryTop5(id));
    }


}
