package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.Book;
import com.example.ebookrecommendsystem.entity.Rating;
import com.example.ebookrecommendsystem.mapper.BookMapper;
import com.example.ebookrecommendsystem.mapper.RatingMapper;
import com.example.ebookrecommendsystem.vo.RecommendedBookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecommendService {

    @Autowired
    private RatingMapper ratingMapper;
    @Autowired
    private BookMapper bookMapper;

    private final Map<Integer, List<RecommendedBookVO>> recommendCache = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 1800000)
    public void refreshRecommendCache() {
        List<Rating> allRatings = ratingMapper.findAllRatings();
        if (allRatings == null || allRatings.isEmpty()) return;

        Set<Integer> userIds = new HashSet<>();
        for (Rating r : allRatings) {
            userIds.add(r.getUserId());
        }

        Map<Integer, List<RecommendedBookVO>> newCache = new ConcurrentHashMap<>();
        for (Integer userId : userIds) {
            List<RecommendedBookVO> books = computeRecommend(userId, allRatings);
            if (!books.isEmpty()) {
                newCache.put(userId, books);
            }
        }
        recommendCache.clear();
        recommendCache.putAll(newCache);
    }

    public Result<List<RecommendedBookVO>> getRecommendBooks(Integer targetUserId) {
        List<RecommendedBookVO> cached = recommendCache.get(targetUserId);
        if (cached != null) {
            return Result.success(cached, "为您生成专属个性化推荐成功！");
        }

        List<Rating> allRatings = ratingMapper.findAllRatings();
        List<RecommendedBookVO> result = computeRecommend(targetUserId, allRatings);

        if (result.isEmpty()) {
            result = getHotBooksForColdStart();
            return Result.success(result, "为您推荐热门高分图书！");
        }

        return Result.success(result, "为您生成专属个性化推荐成功！");
    }

    private List<RecommendedBookVO> getHotBooksForColdStart() {
        List<Book> hotBooks = bookMapper.findHotBooks(5);
        List<RecommendedBookVO> result = new ArrayList<>();
        for (Book book : hotBooks) {
            result.add(new RecommendedBookVO(book, "热门高分图书"));
        }
        return result;
    }

    private List<RecommendedBookVO> computeRecommend(Integer targetUserId, List<Rating> allRatings) {
        if (allRatings == null || allRatings.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, Map<Integer, Double>> userItemScores = new HashMap<>();
        for (Rating r : allRatings) {
            userItemScores.putIfAbsent(r.getUserId(), new HashMap<>());
            userItemScores.get(r.getUserId()).put(r.getBookId(), r.getScore());
        }

        Map<Integer, Double> targetUserScores = userItemScores.get(targetUserId);
        if (targetUserScores == null || targetUserScores.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, Map<Integer, Integer>> itemCoOccurrence = new HashMap<>();
        Map<Integer, Integer> itemCount = new HashMap<>();

        for (Map<Integer, Double> userScores : userItemScores.values()) {
            for (Integer bookId1 : userScores.keySet()) {
                itemCount.put(bookId1, itemCount.getOrDefault(bookId1, 0) + 1);
                itemCoOccurrence.putIfAbsent(bookId1, new HashMap<>());
                for (Integer bookId2 : userScores.keySet()) {
                    if (bookId1.equals(bookId2)) continue;
                    itemCoOccurrence.get(bookId1).put(bookId2,
                            itemCoOccurrence.get(bookId1).getOrDefault(bookId2, 0) + 1);
                }
            }
        }

        Map<Integer, Double> predictScores = new HashMap<>();
        Map<Integer, Integer> reasonBookMap = new HashMap<>();

        for (Integer candidateBookId : itemCount.keySet()) {
            if (targetUserScores.containsKey(candidateBookId)) continue;

            double totalScore = 0.0;
            double maxContribution = 0.0;
            int bestReasonBookId = -1;

            for (Map.Entry<Integer, Double> entry : targetUserScores.entrySet()) {
                Integer readBookId = entry.getKey();
                Double readBookScore = entry.getValue();

                Integer coCount = 0;
                if (itemCoOccurrence.containsKey(candidateBookId)
                        && itemCoOccurrence.get(candidateBookId).containsKey(readBookId)) {
                    coCount = itemCoOccurrence.get(candidateBookId).get(readBookId);
                }

                if (coCount > 0) {
                    double similarity = coCount
                            / Math.sqrt(itemCount.get(candidateBookId) * itemCount.get(readBookId));
                    double contribution = similarity * readBookScore;
                    totalScore += contribution;
                    if (contribution > maxContribution) {
                        maxContribution = contribution;
                        bestReasonBookId = readBookId;
                    }
                }
            }
            if (totalScore > 0) {
                predictScores.put(candidateBookId, totalScore);
                reasonBookMap.put(candidateBookId, bestReasonBookId);
            }
        }

        List<Map.Entry<Integer, Double>> sortedList = new ArrayList<>(predictScores.entrySet());
        sortedList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        Map<Integer, Book> reasonBookCache = new HashMap<>();
        java.util.Set<Integer> reasonBookIds = new java.util.HashSet<>();
        for (Integer rid : reasonBookMap.values()) {
            if (rid != null && rid > 0) reasonBookIds.add(rid);
        }
        if (!reasonBookIds.isEmpty()) {
            List<Book> reasonBooks = bookMapper.findByIds(new ArrayList<>(reasonBookIds));
            for (Book b : reasonBooks) {
                reasonBookCache.put(b.getId(), b);
            }
        }

        // 批量查询推荐图书
        List<Integer> recommendBookIds = new ArrayList<>();
        int limit = 5;
        for (int i = 0; i < Math.min(limit, sortedList.size()); i++) {
            recommendBookIds.add(sortedList.get(i).getKey());
        }
        Map<Integer, Book> recommendBookCache = new HashMap<>();
        if (!recommendBookIds.isEmpty()) {
            List<Book> books = bookMapper.findByIds(recommendBookIds);
            for (Book b : books) {
                recommendBookCache.put(b.getId(), b);
            }
        }

        List<RecommendedBookVO> finalRecommendBooks = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedList.size()); i++) {
            Integer bookId = sortedList.get(i).getKey();
            Book book = recommendBookCache.get(bookId);
            if (book != null) {
                Integer reasonBookId = reasonBookMap.get(bookId);
                String reason;
                Book reasonBook = reasonBookCache.get(reasonBookId);
                if (reasonBook != null) {
                    reason = "因为您读过《" + reasonBook.getTitle() + "》";
                } else {
                    reason = "根据您的阅读偏好推荐";
                }
                finalRecommendBooks.add(new RecommendedBookVO(book, reason));
            }
        }
        return finalRecommendBooks;
    }
}
