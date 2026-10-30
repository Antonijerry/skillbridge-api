package com.skillbridge.auth;

import java.util.UUID;

import com.skillbridge.support.AbstractIntegrationTest;
import com.skillbridge.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFailureTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String registerJson(String name, String email, String password) {
        return "{\"fullName\":\"" + name + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void wrongPasswordReturns401InvalidCredentials() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAuth.loginJson(TestAuth.ADA, "Not-The-Password-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unknownEmailReturnsTheSame401AsWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(TestAuth.loginJson("nobody-" + UUID.randomUUID() + "@example.com",
                                "Whatever-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithBlankFieldsReturns400ValidationError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").exists());
    }

    @Test
    void registerWithBadEmailAndShortPasswordReturns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test User", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerWithPasswordLongerThan72Returns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test User",
                                "long-" + UUID.randomUUID() + "@example.com", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void duplicateEmailReturns409EvenWithDifferentCaseAndSpaces() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test User", email, "Passw0rd-Test")))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Other User", "  " + email.toUpperCase() + " ",
                                "Passw0rd-Test")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    void garbageBearerTokenReturns401Unauthorized() throws Exception {
        mvc.perform(get("/api/skills").header("Authorization", "Bearer this.is.not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}