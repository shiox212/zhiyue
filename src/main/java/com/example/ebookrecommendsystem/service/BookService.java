package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Book;
import com.example.ebookrecommendsystem.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    public Result<List<Book>> getAllBooks() {
        List<Book> books = bookMapper.findAll();
        return Result.success(books, "获取图书列表成功");
    }

    public Result<Book> getBookById(Integer id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            return Result.error(404, "图书不存在或已下架");
        }
        return Result.success(book, "获取图书详情成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> addBook(Book book) {
        bookMapper.insertBook(book);
        return Result.success(null, "图书上架成功");
    }

    public Result<List<Book>> searchBooks(String keyword) {
        List<Book> books = bookMapper.searchByKeyword(keyword);
        return Result.success(books, "搜索成功");
    }

    public Result<List<Book>> searchBooksInCategory(String keyword, String category) {
        List<Book> books = bookMapper.searchByKeywordAndCategory(keyword, category);
        return Result.success(books, "分类搜索成功");
    }

    public Result<List<String>> getAllCategories() {
        List<String> categories = bookMapper.findAllCategories();
        return Result.success(categories, "获取分类列表成功");
    }

    private static final java.util.List<String> FIXED_CATEGORIES = java.util.Arrays.asList(
            "科幻小说", "历史传记", "悬疑推理", "武侠仙侠", "言情小说",
            "都市生活", "奇幻魔幻", "军事战争", "文学经典", "哲学心理"
    );

    public Result<List<java.util.Map<String, Object>>> getCategoriesWithCount() {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (String cat : FIXED_CATEGORIES) {
            int count = bookMapper.countByCategory(cat);
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("name", cat);
            m.put("count", count);
            result.add(m);
        }
        return Result.success(result, "获取分类统计成功");
    }

    public Result<List<Book>> getBooksByCategory(String category) {
        List<Book> books = bookMapper.findByCategory(category);
        return Result.success(books, "获取分类图书成功");
    }

    public Result<List<Book>> getSimilarBooks(Integer bookId) {
        List<Book> books = bookMapper.findSimilarBooks(bookId, 5);
        return Result.success(books, "获取相似图书成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateBook(Book book) {
        bookMapper.updateBook(book);
        return Result.success(null, "图书更新成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> deleteBook(Integer id) {
        bookMapper.deleteById(id);
        return Result.success(null, "图书删除成功");
    }

    public Result<List<Book>> getRanking(String sort) {
        List<Book> books;
        if ("popularity".equals(sort)) {
            books = bookMapper.findAllByPopularityRanking();
        } else {
            books = bookMapper.findAllByRatingRanking();
        }
        return Result.success(books, "获取排行成功");
    }
}
