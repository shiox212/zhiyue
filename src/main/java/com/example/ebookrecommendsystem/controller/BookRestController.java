package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Book;
import com.example.ebookrecommendsystem.mapper.StatsMapper;
import com.example.ebookrecommendsystem.service.BookService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService bookService;
    private final StatsMapper statsMapper;

    public BookRestController(BookService bookService, StatsMapper statsMapper) {
        this.bookService = bookService;
        this.statsMapper = statsMapper;
    }

    @GetMapping
    public Result<List<Book>> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String category) {
        if (q != null && !q.isBlank()) {
            return category == null || category.isBlank()
                    ? bookService.searchBooks(q.trim())
                    : bookService.searchBooksInCategory(q.trim(), category);
        }
        return category == null || category.isBlank()
                ? bookService.getAllBooks()
                : bookService.getBooksByCategory(category);
    }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "返回图书详情", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "图书不存在或已下架", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class), examples = @ExampleObject(value = "{\"code\":404,\"msg\":\"图书不存在或已下架\",\"data\":null}")))
    public ResponseEntity<Result<Book>> get(@PathVariable Integer id) {
        Result<Book> result = bookService.getBookById(id);
        return result.getCode() == 200
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return bookService.getAllCategories();
    }

    @GetMapping("/rankings")
    public Result<List<Book>> rankings(@RequestParam(defaultValue = "rating") String sort) {
        return bookService.getRanking(sort);
    }

    @GetMapping("/{id}/similar")
    public Result<List<Book>> similar(@PathVariable Integer id) {
        return bookService.getSimilarBooks(id);
    }

    @GetMapping("/{id}/rating-distribution")
    public Result<List<Map<String, Object>>> ratingDistribution(@PathVariable Integer id) {
        return Result.success(statsMapper.getRatingDistribution(id), "获取评分分布成功");
    }
}
