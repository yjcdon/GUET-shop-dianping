package com.GUET.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.GUET.constants.SystemConstants;
import com.GUET.dto.UserDTO;
import com.GUET.entity.Blog;
import com.GUET.result.Result;
import com.GUET.service.IBlogService;
import com.GUET.utils.UserHolderUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result saveBlog (@RequestBody Blog blog) {
        return blogService.saveBlogAndPush(blog);
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

    @GetMapping("/of/user")
    public Result queryBlogByUserId (
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/2 17:01:40
     * @Params: 下次滚动查询的最大时间戳，跳过时间戳相同的数据个数
     * @Return:
     * @Description: 查询关注的博主发的笔记，使用滚动分页查询
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow (@RequestParam("lastId") Long maxTime,
                                     @RequestParam(defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(maxTime,offset);

    }

}
