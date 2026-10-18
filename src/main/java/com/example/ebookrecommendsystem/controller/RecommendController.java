package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.security.Identity;
import com.example.ebookrecommendsystem.service.RecommendService;
import com.example.ebookrecommendsystem.vo.RecommendedBookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/get")
    public Result<List<RecommendedBookVO>> getRecommend(@RequestParam Integer userId) {
        return recommendService.getRecommendBooks(Identity.own(userId));
    }
}