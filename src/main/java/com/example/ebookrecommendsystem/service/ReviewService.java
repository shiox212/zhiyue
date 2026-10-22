package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Review;
import com.example.ebookrecommendsystem.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> addReview(Review review) {
        if (review.getContent() == null || review.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空哦！");
        }
        reviewMapper.insertReview(review);
        return Result.success(null, "发布书评成功！");
    }

    public Result<List<Map<String, Object>>> getBookReviews(Integer bookId) {
        return Result.success(reviewMapper.findReviewsByBookId(bookId), "获取评论成功");
    }

    public Result<List<Map<String, Object>>> getAllReviews() {
        return Result.success(reviewMapper.findAllReviews(), "获取全部评论成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> auditReview(Integer id, Integer status) {
        reviewMapper.updateStatus(id, status);
        return Result.success(null, status == 1 ? "评论已通过审核" : "评论已驳回");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> likeReview(Integer reviewId, Integer userId) {
        int exist = reviewMapper.countLike(reviewId, userId);
        if (exist > 0) {
            return Result.error("您已经点过赞了");
        }
        reviewMapper.insertLike(reviewId, userId);
        reviewMapper.incrLikeCount(reviewId);
        return Result.success(null, "点赞成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> unlikeReview(Integer reviewId, Integer userId) {
        // Only the transaction that actually removes a row may decrement the counter.
        if (reviewMapper.deleteLike(reviewId, userId) == 0) {
            return Result.error("您还没有点赞");
        }
        reviewMapper.decrLikeCount(reviewId);
        return Result.success(null, "已取消点赞");
    }

    public List<Integer> getLikedReviewIds(Integer userId) {
        return reviewMapper.findLikedReviewIds(userId);
    }
}
