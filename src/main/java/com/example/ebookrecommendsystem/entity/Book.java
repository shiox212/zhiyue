package com.example.ebookrecommendsystem.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Date;

@Data
public class Book {
    private Integer id;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200位")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 50, message = "作者长度不能超过50位")
    private String author;

    @NotBlank(message = "分类不能为空")
    @Size(max = 30, message = "分类长度不能超过30位")
    private String category;

    @Size(max = 5000, message = "封面链接长度不能超过5000位")
    private String coverUrl;

    @Size(max = 2000, message = "简介长度不能超过2000位")
    private String description;

    private Date createTime;

    private Double avgScore;
    private Integer ratingCount;
    private Integer reviewCount;
    private Integer rankNum;
}