package com.guet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guet.annotation.JudgeIdExist;
import com.guet.constants.SystemConstants;
import com.guet.entity.Shop;
import com.guet.mapper.ShopMapper;
import com.guet.result.Result;
import com.guet.service.IShopService;
import com.guet.utils.MyRedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.guet.constants.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private ShopMapper shopMapper;

    private Random random = new Random();

    @Autowired
    private MyRedisUtils redisUtils;

    @Override
    @JudgeIdExist(key = "shopIds")// 记得提前将店铺数据加载到Redis中，不然查不出来
    public Result queryById (Long id) throws Exception {
        // 逻辑删除解决缓存击穿
        Shop shop = redisUtils.getWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class,
                CACHE_SHOP_TTL + random.nextInt(5), TimeUnit.MINUTES,
                shopID -> shopMapper.selectById(shopID));

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }

        return Result.ok(shop);
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/18 10:59:57
     * @Description: 新增一个店铺
     */
    @Override
    @Transactional
    public Result saveShopAndDelCache (Shop shop) {
        // 先修改数据库
        shopMapper.insert(shop);

        // 删除缓存
        srt.delete(CACHE_SHOP_KEY);

        return Result.ok();
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/18 10:59:40
     * @Description: 更新一个店铺的数据
     */
    @Override
    @Transactional
    public Result updateByIdAndDelCache (Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺不存在！");
        }

        // 要保证数据一致性，控制事务；
        // 如果是微服务，这里只做数据库更新，Redis在另一个服务中，需要MQ或者TTC等工具

        // 先更新数据库
        shopMapper.updateById(shop);

        // 再删除缓存
        srt.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok("更新成功！");
    }


    @Override
    public Result queryShopByType (Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询，用户没有开启定位，可以按照IP所在地查出附近的商家
        if (x == null || y == null) {
            // 不需要坐标查询，直接去数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = srt.opsForGeo()
                .search(key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),// 单位是m
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                .includeDistance()
                                .limit(end)
                );

        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= start) {
            // 没有下一页了，结束；因为在下面你执行skip，如果跳过的值大于开始查询的索引，那List就是空了
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(start).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });

        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")")
                .list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        // 6.返回
        return Result.ok(shops);
    }
}
