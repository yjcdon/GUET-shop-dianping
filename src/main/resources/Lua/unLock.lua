-- 获取线程标识的key
local id = redis.call('get',KEYS[1])

-- 比较线程标识和锁的value是否一致
if(id == ARGV[1]) then
    return redis.call('del',KEYS[1])
end
-- 不一致则返回0
return 0