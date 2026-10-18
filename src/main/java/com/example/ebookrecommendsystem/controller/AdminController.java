package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Book;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.service.BookService;
import com.example.ebookrecommendsystem.service.ReviewService;
import com.example.ebookrecommendsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.example.ebookrecommendsystem.security.AccountSessions sessions;

    @Autowired
    private ReviewService reviewService;

    // ========== 图书管理 ==========
    @PostMapping("/book/add")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('book:manage')")
    public Result<Object> addBook(@RequestBody Book book) {
        return bookService.addBook(book);
    }

    @PostMapping("/book/update")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('book:manage')")
    public Result<Object> updateBook(@RequestBody Book book) {
        return bookService.updateBook(book);
    }

    @PostMapping("/book/delete")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('book:manage')")
    public Result<Object> deleteBook(@RequestBody Map<String, Integer> params) {
        return bookService.deleteBook(params.get("id"));
    }

    // ========== 用户管理 ==========
    @GetMapping("/user/list")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('user:manage')")
    public Result<List<User>> userList() {
        return userService.getAllUsers();
    }

    @PostMapping("/user/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('user:manage')")
    public Result<Object> updateUserStatus(@RequestBody Map<String, Integer> params) {
        return sessions.updateStatus(params.get("id"), params.get("status"));
    }

    @PostMapping("/user/delete")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('user:manage')")
    public Result<Object> deleteUser(@RequestBody Map<String, Integer> params) {
        return userService.deleteUser(params.get("id"));
    }

    // ========== 书评审核 ==========
    @GetMapping("/review/list")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('review:moderate')")
    public Result<List<Map<String, Object>>> reviewList() {
        return reviewService.getAllReviews();
    }

    @PostMapping("/review/audit")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('review:moderate')")
    public Result<Object> auditReview(@RequestBody Map<String, Integer> params) {
        return reviewService.auditReview(params.get("id"), params.get("status"));
    }
}
