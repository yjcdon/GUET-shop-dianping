package com.GUET.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.GUET.dto.UserDTO;
import com.GUET.entity.User;
import com.GUET.result.Result;

import java.util.List;

/**
* @author lyj
* @description 针对表【tb_user】的数据库操作Mapper
* @createDate 2024-02-15 18:09:23
* @Entity  com.hmdp.entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    List<UserDTO> queryCommon (List<Long> userIdList);

    Result signCount ();
}




