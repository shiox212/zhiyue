package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.security.Identity;
import com.example.ebookrecommendsystem.entity.Bookshelf;
import com.example.ebookrecommendsystem.service.BookshelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookshelf")
public class BookshelfController {

    @Autowired
    private BookshelfService bookshelfService;

    // 添加到书架 / 修改状态
    @PostMapping("/add")
    public Result<Object> add(@RequestBody Bookshelf bookshelf) {
        bookshelf.setUserId(Identity.own(bookshelf.getUserId()));
        return bookshelfService.addToShelf(bookshelf);
    }

    @GetMapping("/list")
    public Result<java.util.List<java.util.Map<String, Object>>> list(@RequestParam Integer userId) {
        // 规范调用 Service 层的方法
        return Result.success(bookshelfService.getBookshelfList(Identity.own(userId)), "获取书架成功");
    }

    @PostMapping("/remove")
    public Result<Object> remove(@RequestBody java.util.Map<String, Integer> params) {
        return bookshelfService.removeFromShelf(Identity.own(params.get("userId")), params.get("bookId"));
    }

}