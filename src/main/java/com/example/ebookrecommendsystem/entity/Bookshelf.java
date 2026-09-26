package com.example.ebookrecommendsystem.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Bookshelf {
    private Integer id;
    private Integer userId;
    private Integer bookId;
    private Integer readStatus;
    private Date createTime;
}