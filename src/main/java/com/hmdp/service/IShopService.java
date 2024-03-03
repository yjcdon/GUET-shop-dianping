package com.hmdp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.result.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryById (Long id) throws Exception;

    Result saveShopAndDelCache (Shop shop);

    Result updateByIdAndDelCache (Shop shop);

    Result queryShopByType (Integer typeId, Integer current, Double x, Double y);
}
