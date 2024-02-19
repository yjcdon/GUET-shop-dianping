package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constants.SystemConstants;
import com.hmdp.dto.RedisDataDTO;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.result.Result;
import com.hmdp.service.IShopService;
import com.hmdp.utils.MyConvertUtils;
import com.hmdp.utils.MyRedisUtils;
import com.sun.deploy.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constants.RedisConstants.*;

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
    public Page<Shop> queryPage (Integer typeId, Page<Shop> page) {

        String key = CACHE_SHOP_LIST_KEY + typeId;

        // 先查缓存
        // 这个page.getCurrent和请求的current是一样的
        List<String> shopList = srt.opsForList().range(key,
                (page.getCurrent() - 1) * SystemConstants.DEFAULT_PAGE_SIZE,
                page.getCurrent() * SystemConstants.DEFAULT_PAGE_SIZE);

        // 如果结果不为空，直接返回
        if (shopList != null && !shopList.isEmpty()) {
            // 拼接成标准JSON数组字符串
            String json = "[" + StringUtils.join(shopList, ",") + "]";
            page.setRecords(JSONUtil.toList(json, Shop.class));
            return page;
        }

        // 查不到，去数据库查
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getTypeId, typeId);

        page.setCurrent(page.getCurrent());
        Page<Shop> shopPage = shopMapper.selectPage(page, queryWrapper);
        List<Shop> records = shopPage.getRecords();

        if (records != null && !records.isEmpty()) {
            // 这个前端有点问题，明明都没有数据了，你往下拉，发的请求current还是会加1；
            // 所以会查出空数据，就要来做判断
            // 这就是缓存穿透吧
            srt.opsForList().rightPushAll(key,
                    MyConvertUtils.objectListToStringList(records, shop -> JSONUtil.toJsonStr(shop)));
            // 添加随机时间，防止缓存雪崩
            srt.expire(key, CACHE_SHOP_TTL + random.nextInt(5), TimeUnit.MINUTES);
        } else {
            // 缓存空值
            srt.opsForList().rightPush(key, "");
            srt.expire(key, CACHE_NULL_TTL, TimeUnit.MINUTES);
        }

        // 返回结果
        return shopPage;
    }
}
