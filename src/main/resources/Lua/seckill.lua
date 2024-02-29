-- 判断用户是否具有购买资格

-- 参数列表
-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]

-- 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'seckill:order:' .. voucherId

-- Redis业务
-- 判断库存是否充足，使用这个函数执将返回值转为数字
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，不允许下单，返回1
    return 1
end

-- 判断用户是否下过单,sismember判断已下单集合中是否有用户ID
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 存在用户，是重复下单，不允许下单，返回2
    return 2
end

-- 有购买资格，扣减库存
redis.call('incrby', stockKey, -1)

-- 将userId保存到已下单集合中
redis.call('sadd', orderKey, userId)

-- 返回0
return 0