package com.example.ebookrecommendsystem.vo;

import com.example.ebookrecommendsystem.entity.Book;
import lombok.Data;

@Data
public class RecommendedBookVO {
    private Integer id;
    private String title;
    private String author;
    private String category;
    private String coverUrl;
    private String description;
    private String reason;

    public RecommendedBookVO() {}

    public RecommendedBookVO(Book book, String reason) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.category = book.getCategory();
        this.coverUrl = book.getCoverUrl();
        this.description = book.getDescription();
        this.reason = reason;
    }
}
