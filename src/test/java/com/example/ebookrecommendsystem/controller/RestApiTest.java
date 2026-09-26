package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.service.BookService;
import com.example.ebookrecommendsystem.service.RecommendService;
import com.example.ebookrecommendsystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class RestApiTest {
    @Autowired MockMvc mvc;
    @MockBean BookService books;
    @MockBean UserService users;
    @MockBean RecommendService recommendations;

    @Test void missingBookReturns404() throws Exception {
        when(books.getBookById(999)).thenReturn(Result.error(404, "图书不存在"));
        mvc.perform(get("/api/books/999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404));
    }
    @Test void invalidIdReturns400() throws Exception {
        mvc.perform(get("/api/books/not-a-number")).andExpect(status().isBadRequest());
    }
    @Test void corsAllowsConfiguredOriginAndCredentials() throws Exception {
        mvc.perform(options("/api/session").header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    @Test void corsRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/session").header("Origin", "https://invalid.example")
            .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    @Test void sessionRequiresLogin() throws Exception {
        mvc.perform(get("/api/session")).andExpect(status().isUnauthorized());
    }
    @Test void invalidLoginBodyReturns400() throws Exception {
        mvc.perform(post("/api/session").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
    @Test void loginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        User input = new User(); input.setUsername("reader"); input.setPassword("secret123");
        User user = new User(); user.setId(7); user.setUsername("reader");
        when(users.login(input)).thenReturn(Result.success(user, "ok"));
        MockHttpSession old = new MockHttpSession();
        var result = mvc.perform(post("/api/session").session(old).contentType("application/json")
            .content("{\"username\":\"reader\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk()).andReturn();
        assertTrue(old.isInvalid());
        MockHttpSession fresh = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(fresh);
        mvc.perform(get("/api/session").session(fresh)).andExpect(jsonPath("$.data.id").value(7));
        mvc.perform(delete("/api/session").session(fresh)).andExpect(status().isOk());
        assertTrue(fresh.isInvalid());
    }
    @Test void openApiIsPublicAndContainsRestRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/books']").exists())
            .andExpect(jsonPath("$.paths['/api/session']").exists())
            .andExpect(jsonPath("$.paths['/api/session'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/session'].post.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/books/{id}'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/session'].get.responses['401']").exists())
            .andExpect(jsonPath("$.paths['/api/books/{id}'].get.responses['404']").exists())
            .andExpect(jsonPath("$.components.schemas.User.properties.password.writeOnly").value(true));
    }

    @Test void passwordIsNeverSerializedEvenIfServiceReturnsIt() throws Exception {
        User input = new User(); input.setUsername("reader"); input.setPassword("secret123");
        User returned = new User(); returned.setId(7); returned.setUsername("reader");
        returned.setPassword("must-not-appear-in-json");
        when(users.login(input)).thenReturn(Result.success(returned, "ok"));
        mvc.perform(post("/api/session").contentType("application/json")
            .content("{\"username\":\"reader\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test void badCredentialsReturn401() throws Exception {
        User input = new User(); input.setUsername("reader"); input.setPassword("wrong123");
        when(users.login(input)).thenReturn(Result.error("密码错误"));
        mvc.perform(post("/api/session").contentType("application/json")
            .content("{\"username\":\"reader\",\"password\":\"wrong123\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }
}
