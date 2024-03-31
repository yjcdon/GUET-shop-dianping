package com.guet.controller;


import com.guet.result.Result;
import com.guet.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/2 10:38:17
     * @Params: 博主的id，关注或者不关注
     * @Return:
     * @Description: 实现关注和取关的接口
     */

    @PutMapping("/{id}/{isFollow}")
    public Result followOrNot (@PathVariable Long id, @PathVariable Boolean isFollow) {
        return followService.followOrNot(id, isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result queryIsFollow (@PathVariable Long id) {
        return followService.queryIsFollow(id);
    }

    @GetMapping("/common/{id}")
    public Result queryCommon(@PathVariable Long id){
        return followService.queryCommon(id);
    }

}
