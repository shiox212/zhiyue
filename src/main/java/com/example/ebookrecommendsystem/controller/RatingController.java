package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Rating;
import com.example.ebookrecommendsystem.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rating")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping("/add")
    public Result<Object> add(@RequestBody @Valid Rating rating) {
        return ratingService.rateBook(rating);
    }

    @GetMapping("/get")
    public Result<Double> getRating(@RequestParam Integer userId, @RequestParam Integer bookId) {
        return ratingService.getUserRating(userId, bookId);
    }
}