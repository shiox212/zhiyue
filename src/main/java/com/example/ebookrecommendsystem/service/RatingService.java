package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Rating;
import com.example.ebookrecommendsystem.mapper.RatingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    @Autowired
    private RatingMapper ratingMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> rateBook(Rating rating) {
        if (rating.getScore() == null || rating.getScore() < 0 || rating.getScore() > 5) {
            return Result.error("打分失败：分数必须在 0 到 5 之间！");
        }

        Rating existRating = ratingMapper.findByUserIdAndBookId(rating.getUserId(), rating.getBookId());

        if (existRating != null) {
            existRating.setScore(rating.getScore());
            ratingMapper.updateScore(existRating);
            return Result.success(null, "修改评分成功！");
        } else {
            ratingMapper.insertRating(rating);
            return Result.success(null, "评价成功，感谢您的打分！");
        }
    }

    public Result<Double> getUserRating(Integer userId, Integer bookId) {
        Rating rating = ratingMapper.findByUserIdAndBookId(userId, bookId);
        if (rating != null) {
            return Result.success(rating.getScore(), "获取评分成功");
        }
        return Result.success(0.0, "暂无评分"); // 如果没打过分，返回 0.0
    }
}