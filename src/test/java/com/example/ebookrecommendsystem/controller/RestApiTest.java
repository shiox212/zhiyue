package com.example.ebookrecommendsystem.controller;
import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.service.BookService;
import com.example.ebookrecommendsystem.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
class RestApiTest {
    @Autowired MockMvc mvc;
    @MockBean BookService books;
    @MockBean AccountSessions sessions;
    @Test void missingBookReturns404() throws Exception {
        when(books.getBookById(999)).thenReturn(Result.error(404,"图书不存在"));
        mvc.perform(get("/api/books/999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
    }
    @Test void invalidIdReturns400() throws Exception { mvc.perform(get("/api/books/not-a-number")).andExpect(status().isBadRequest()); }
    @Test void corsAllowsConfiguredOriginAndCredentials() throws Exception {
        mvc.perform(options("/api/session").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type,x-xsrf-token"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173")).andExpect(header().string("Access-Control-Allow-Credentials","true"));
    }
    @Test void corsRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/session").header("Origin","https://invalid.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
    @Test void sessionRequiresLogin() throws Exception { mvc.perform(get("/api/session")).andExpect(status().isUnauthorized()); }
    @Test void invalidLoginBodyReturns400() throws Exception { mvc.perform(post("/api/session").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest()); }
    @Test void loginUsesHttpOnlyCookiesWithoutCreatingSession() throws Exception {
        User user=new User();user.setId(7);user.setUsername("reader");
        when(sessions.login(any())).thenReturn(new AccountSessions.Tokens(user,"signed-token-test-fixture","opaque-refresh-test-fixture"));
        var response=mvc.perform(post("/api/session").with(csrf()).contentType("application/json").content("{\"username\":\"reader\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk()).andExpect(cookie().httpOnly("ZY_ACCESS",true)).andExpect(cookie().httpOnly("ZY_REFRESH",true))
            .andExpect(jsonPath("$.data.id").value(7)).andExpect(jsonPath("$.data.access").doesNotExist()).andReturn();
        assertNull(response.getRequest().getSession(false));
    }
    @Test void openApiIsPublicAndContainsRestRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/books']").exists())
            .andExpect(jsonPath("$.paths['/api/session'].get.responses['200']").exists()).andExpect(jsonPath("$.paths['/api/session'].get.responses['401']").exists())
            .andExpect(jsonPath("$.paths['/api/books/{id}'].get.responses['404']").exists()).andExpect(jsonPath("$.components.schemas.User.properties.password.writeOnly").value(true));
    }
    @Test void passwordNeverSerializedEvenIfServiceReturnsIt() throws Exception {
        User user=new User();user.setId(7);user.setPassword("never-return-this");
        when(sessions.login(any())).thenReturn(new AccountSessions.Tokens(user,"test-access","test-refresh"));
        mvc.perform(post("/api/session").with(csrf()).contentType("application/json").content("{\"username\":\"reader\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.password").doesNotExist());
    }
    @Test void badCredentialsReturn401() throws Exception {
        when(sessions.login(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("invalid"));
        mvc.perform(post("/api/session").with(csrf()).contentType("application/json").content("{\"username\":\"reader\",\"password\":\"wrong123\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }
}
