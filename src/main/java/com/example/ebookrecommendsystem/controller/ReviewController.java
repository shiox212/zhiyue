package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Review;
import com.example.ebookrecommendsystem.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/add")
    public Result<Object> add(@RequestBody @Valid Review review) {
        return reviewService.addReview(review);
    }

    @GetMapping("/list")
    public Result<java.util.List<java.util.Map<String, Object>>> list(@RequestParam Integer bookId) {
        return reviewService.getBookReviews(bookId);
    }

    @PostMapping("/like")
    public Result<Object> like(@RequestBody java.util.Map<String, Integer> params) {
        return reviewService.likeReview(params.get("reviewId"), params.get("userId"));
    }

    @PostMapping("/unlike")
    public Result<Object> unlike(@RequestBody java.util.Map<String, Integer> params) {
        return reviewService.unlikeReview(params.get("reviewId"), params.get("userId"));
    }

    @GetMapping("/liked")
    public Result<java.util.List<Integer>> likedIds(@RequestParam Integer userId) {
        return Result.success(reviewService.getLikedReviewIds(userId), "获取点赞记录成功");
    }
}