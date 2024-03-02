package com.hmdp.constants;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 3L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 43200L; // 单位是min的话，为一个月

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType";

    public static final String CACHE_SHOP_LIST_KEY = "cache:shop:typeID";

    public static final Long CACHE_SHOP_LOGICAL_TTL = 60L;// 60s

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final String LOCK_KEY = "lock:";
    public static final Long LOCK_SHOP_TTL = 10L;
    public static final String CACHE_ORDER_KEY = "order:";

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String BLOG_DETAIL_KEY = "blog:";

    public static final String FOLLOWS_KEY = "follows:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
