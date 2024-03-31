package com.guet.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScrollResultDTO {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
