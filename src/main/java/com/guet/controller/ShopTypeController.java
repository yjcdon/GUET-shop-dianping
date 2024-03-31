package com.guet.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.guet.result.Result;
import com.guet.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService shopTypeService;

    @GetMapping("list")
    public Result queryShopTypeList () throws JsonProcessingException {
        return Result.ok(shopTypeService.queryByCacheOrList());
    }
}
