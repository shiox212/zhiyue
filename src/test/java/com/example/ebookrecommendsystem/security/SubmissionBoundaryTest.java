package com.example.ebookrecommendsystem.security;

import com.example.ebookrecommendsystem.mapper.ReviewMapper;
import com.example.ebookrecommendsystem.mapper.SecurityMapper;
import com.example.ebookrecommendsystem.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:submissiontest;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER")
@AutoConfigureMockMvc
@Sql(scripts={"classpath:security-fixture.sql","file:database/migrations/V004__security.sql"})
@SqlConfig(encoding="UTF-8")
class SubmissionBoundaryTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;
    @Autowired SecurityMapper security;
    @Autowired ObjectMapper json;
    @Autowired ReviewService reviews;
    @SpyBean ReviewMapper reviewMapper;
    Cookie token(int id){return new Cookie(TokenCookies.ACCESS,jwt.issue(id,security.version(id)));}
    RequestPostProcessor csrf() throws Exception {
        var response=mvc.perform(get("/api/csrf")).andReturn().getResponse();
        Cookie cookie=response.getCookie("XSRF-TOKEN");
        String value=json.readTree(response.getContentAsString()).path("data").path("token").asText();
        return request->{
            var cookies=new java.util.ArrayList<Cookie>();
            if(request.getCookies()!=null)cookies.addAll(java.util.Arrays.asList(request.getCookies()));
            cookies.add(cookie);request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN",value);return request;
        };
    }
    @Test void legacyBookCreationCannotBypassAdminRoleAndPermission()throws Exception{
        String body="{\"title\":\"permission-boundary-book\",\"author\":\"fixture\",\"category\":\"文学经典\"}";
        mvc.perform(post("/api/book/add").with(csrf()).cookie(token(10)).contentType("application/json").content(body))
            .andExpect(status().isForbidden());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM book",Integer.class));
        mvc.perform(post("/api/book/add").with(csrf()).cookie(token(30)).contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        jdbc.update("DELETE FROM security_role_permission WHERE permission_code='book:manage'");
        mvc.perform(post("/api/book/add").with(csrf()).cookie(token(30)).contentType("application/json").content(body))
            .andExpect(status().isForbidden());
    }
    @Test void shelfRejectsUndefinedReadingStatesWithoutWriting()throws Exception{
        for(int state:new int[]{-1,3,99}){
            mvc.perform(post("/api/bookshelf/add").with(csrf()).cookie(token(10)).contentType("application/json")
                .content("{\"userId\":10,\"bookId\":1,\"readStatus\":"+state+"}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM bookshelf",Integer.class));
        mvc.perform(post("/api/bookshelf/add").with(csrf()).cookie(token(10)).contentType("application/json")
            .content("{\"userId\":10,\"bookId\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0,jdbc.queryForObject("SELECT read_status FROM bookshelf WHERE user_id=10",Integer.class));
    }
    @Test void bookDetailReportsStoredRatingsAndOnlyApprovedReviewCount()throws Exception{
        jdbc.update("INSERT INTO rating(user_id,book_id,score) VALUES(10,1,4),(20,1,5)");
        jdbc.update("INSERT INTO review(user_id,book_id,content,status) VALUES(10,1,'published',1),(20,1,'hidden',0)");
        mvc.perform(get("/api/books/1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.avgScore").value(4.5))
            .andExpect(jsonPath("$.data.ratingCount").value(2)).andExpect(jsonPath("$.data.reviewCount").value(1));
    }
    @Test void concurrentUnlikeNeverSubtractsAnotherUsersLike()throws Exception{
        jdbc.update("INSERT INTO review(id,user_id,book_id,content,status,like_count) VALUES(90,10,1,'fixture',1,2)");
        jdbc.update("INSERT INTO review_like(review_id,user_id) VALUES(90,10),(90,20)");
        var bothRead=new CyclicBarrier(2);
        doAnswer(call->{
            int found=jdbc.queryForObject("SELECT COUNT(*) FROM review_like WHERE review_id=90 AND user_id=10",Integer.class);
            bothRead.await(5,TimeUnit.SECONDS);return found;
        }).when(reviewMapper).countLike(eq(90),eq(10));
        var executor=Executors.newFixedThreadPool(2);
        try{
            var a=executor.submit(()->reviews.unlikeReview(90,10));
            var b=executor.submit(()->reviews.unlikeReview(90,10));
            a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM review_like WHERE review_id=90",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT like_count FROM review WHERE id=90",Integer.class));
        }finally{executor.shutdownNow();}
    }
}
