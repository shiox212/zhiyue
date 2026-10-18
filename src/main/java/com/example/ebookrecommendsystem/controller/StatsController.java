package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.mapper.StatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsMapper statsMapper;

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('stats:read')")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> finalData = new HashMap<>();

        finalData.put("totalBooks", statsMapper.countBooks());
        finalData.put("totalUsers", statsMapper.countUsers());
        finalData.put("totalRatings", statsMapper.countRatings());
        finalData.put("categoryData", statsMapper.getCategoryStats());
        finalData.put("topBooksData", statsMapper.getTopRatedBooks());
        finalData.put("userGrowthData", statsMapper.getUserGrowth());

        return Result.success(finalData, "获取看板数据成功");
    }
}