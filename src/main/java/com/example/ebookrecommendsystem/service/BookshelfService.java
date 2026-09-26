package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Bookshelf;
import com.example.ebookrecommendsystem.mapper.BookshelfMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BookshelfService {

    @Autowired
    private BookshelfMapper bookshelfMapper;

    // 添加或更新书架状态
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> addToShelf(Bookshelf bookshelf) {
        Bookshelf existRecord = bookshelfMapper.findByUserIdAndBookId(bookshelf.getUserId(), bookshelf.getBookId());

        if (existRecord != null) {
            existRecord.setReadStatus(bookshelf.getReadStatus());
            bookshelfMapper.updateStatus(existRecord);
            return Result.success(null, "更新阅读状态成功！");
        } else {
            if (bookshelf.getReadStatus() == null) {
                bookshelf.setReadStatus(0); // 默认想读
            }
            bookshelfMapper.insertBookshelf(bookshelf);
            return Result.success(null, "成功加入个人书架！");
        }
    }

    public List<Map<String, Object>> getBookshelfList(Integer userId) {
        return bookshelfMapper.getBookshelfByUserId(userId);
    }

    public Result<Object> removeFromShelf(Integer userId, Integer bookId) {
        bookshelfMapper.deleteBookshelf(userId, bookId);
        return Result.success(null, "已成功移出书架！");
    }
}