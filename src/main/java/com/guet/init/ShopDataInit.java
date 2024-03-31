package com.guet.init;

import com.guet.entity.Shop;
import com.guet.mapper.ShopMapper;
import com.guet.utils.MyRedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.guet.constants.RedisConstants.CACHE_SHOP_KEY;
import static com.guet.constants.RedisConstants.SHOP_GEO_KEY;

@Component
public class ShopDataInit {
    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private MyRedisUtils redisUtils;

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/31 21:30:21
     * @Params:
     * @Return:
     * @Description: 将店铺id保存到Redis中，查询时会先来Redis中判断id是否存在，不存在则无法查询
     */
    @PostConstruct
    public void shopIdsInit () {
        List<Shop> shops = shopMapper.selectList(null);
        String shopidsKey = "shopIds";
        for (Shop shop : shops) {
            Long shopId = shop.getId();
            int hashVal = Math.abs(shopId.hashCode());
            // 缓存店铺ID到Redis的BitMap
            srt.opsForValue().setBit(shopidsKey, hashVal % 100000, true);

            // 缓存店铺数据到Redis
            redisUtils.setWithLogicalExpire(CACHE_SHOP_KEY + shopId, shop, 20L, TimeUnit.MINUTES);
        }
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/3/31 21:31:25
     * @Params:
     * @Return:
     * @Description: 将店铺的地理位置坐标保存到Redis并计算出距离
     */
    @PostConstruct
    public void saveShopGeo () {
        // 查询店铺信息
        List<Shop> shops = shopMapper.selectList(null);

        // 把店铺按照typeId分组，利用Stream进行分组收集成map
        Map<Long, List<Shop>> map = shops.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));

        // 分批写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // typeId
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 该类型的Shop
            List<Shop> value = entry.getValue();

            // 写入Redis的geo,GEOADD key 经度 维度 member
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            for (Shop shop : value) {
                // 这个方法效率不高，会频繁与Redis进行交互
                // srt.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());

                // 这个好
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            srt.opsForGeo().add(key, locations);
        }
    }
}
