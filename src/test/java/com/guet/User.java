package com.guet;

import java.io.IOException;

/**
 * Author: 梁雨佳
 * Date: 2024/2/21 11:59:24
 * Description:
 */

public class User {

    private String name;
    private String age;
    private String gender;

    public String eat (String food) throws IOException, NullPointerException {
        System.out.println("吃" + food);
        return "嗨害,吃饱啦";
    }

    private int sleep () throws RuntimeException {
        System.out.println("睡觉");
        return 666;
    }
}
