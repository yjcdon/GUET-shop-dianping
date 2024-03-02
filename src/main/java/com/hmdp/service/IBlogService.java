package com.hmdp.service;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.result.Result;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById (Long id) throws Exception;

    Result queryHotBlog (Integer current);

    Result likeBlog (Long id);

    List<UserDTO> queryTop5 (Long id);

    Result saveBlogAndPush (Blog blog);

    Result queryBlogOfFollow (Long maxTime, Integer offset);
}
