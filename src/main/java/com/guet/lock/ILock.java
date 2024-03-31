package com.guet.lock;

/**
 * Author: 梁雨佳
 * Date: 2024/2/27 13:07:15
 * Description:
 */
public interface ILock {
    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/27 13:08:01
     * @Description: 尝试获取锁，非阻塞模式
     */

    boolean tryLock (long expireTimeMilis);

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/27 13:08:12
     * @Description: 释放锁
     */
    void unlock ();
}
