package com.guet.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisDataDTO {
    private LocalDateTime expireTime;
    private Object data;
}
