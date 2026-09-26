package com.example.ebookrecommendsystem;

import com.example.ebookrecommendsystem.service.RecommendService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class EbookRecommendSystemApplicationTests {

    @MockBean
    private RecommendService recommendService;

    @Test
    void contextLoads() {
    }

}
