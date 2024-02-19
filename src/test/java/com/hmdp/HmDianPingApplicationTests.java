package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.MyRedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.hmdp.constants.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private MyRedisUtils redisUtils;

    @Autowired
    private IShopService shopService;

    @Test
    public void testSaveShopData () {
        Long id = 1L;
        Shop shop = shopService.getById(1L);
        redisUtils.setWithLogicalExpire(CACHE_SHOP_KEY + id, shop, 20L, TimeUnit.SECONDS);
    }
}
