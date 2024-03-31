package com.GUET.service;

import com.GUET.dto.UserDTO;
import com.GUET.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.GUET.result.Result;

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
