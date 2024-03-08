package com.hmdp.init;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.utils.MyRedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constants.RedisConstants.CACHE_SHOP_KEY;

@Component
public class ShopDataInit {
    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private MyRedisUtils redisUtils;

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
}
