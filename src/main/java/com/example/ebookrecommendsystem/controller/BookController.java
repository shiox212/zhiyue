package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Book;
import com.example.ebookrecommendsystem.mapper.StatsMapper;
import com.example.ebookrecommendsystem.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/book")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private StatsMapper statsMapper;

    @GetMapping("/list")
    public Result<List<Book>> list(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return bookService.getBooksByCategory(category);
        }
        return bookService.getAllBooks();
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return bookService.getAllCategories();
    }

    @GetMapping("/categories-with-count")
    public Result<List<Map<String, Object>>> categoriesWithCount() {
        return bookService.getCategoriesWithCount();
    }

    @PostMapping("/add")
    public Result<Object> add(@RequestBody @Valid Book book) {
        return bookService.addBook(book);
    }

    @GetMapping("/search")
    public Result<List<Book>> search(@RequestParam String keyword,
                                     @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return bookService.searchBooksInCategory(keyword, category);
        }
        return bookService.searchBooks(keyword);
    }

    @GetMapping("/similar")
    public Result<List<Book>> similar(@RequestParam Integer bookId) {
        return bookService.getSimilarBooks(bookId);
    }

    @GetMapping("/rating-distribution")
    public Result<List<Map<String, Object>>> ratingDistribution(@RequestParam Integer bookId) {
        List<Map<String, Object>> data = statsMapper.getRatingDistribution(bookId);
        return Result.success(data, "获取评分分布成功");
    }

    @GetMapping("/ranking")
    public Result<List<Book>> ranking(@RequestParam(defaultValue = "rating") String sort) {
        return bookService.getRanking(sort);
    }
}