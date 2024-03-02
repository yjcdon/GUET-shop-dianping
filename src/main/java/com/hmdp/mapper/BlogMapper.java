package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;

import java.util.List;
import java.util.Set;

/**
* @author lyj
* @description 针对表【tb_blog】的数据库操作Mapper
* @createDate 2024-02-15 18:09:23
* @Entity  com.hmdp.entity.Blog
*/
public interface BlogMapper extends BaseMapper<Blog> {

    List<UserDTO> queryUserDTOByIds (Set<String> userIds);

}




