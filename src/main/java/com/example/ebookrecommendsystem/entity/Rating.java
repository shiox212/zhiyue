package com.example.ebookrecommendsystem.entity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Date;

@Data
public class Rating {
    private Integer id;

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotNull(message = "图书ID不能为空")
    private Integer bookId;

    @NotNull(message = "评分不能为空")
    private Double score;

    private Date createTime;
}