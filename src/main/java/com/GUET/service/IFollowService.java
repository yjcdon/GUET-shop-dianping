package com.GUET.service;

import com.GUET.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.GUET.result.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result followOrNot (Long id, Boolean isFollow);

    Result queryIsFollow (Long id);

    Result queryCommon (Long id);
}
