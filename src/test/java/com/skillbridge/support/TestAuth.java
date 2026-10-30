package com.skillbridge.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAuth {

    public static final String PASSWORD = "Demo-Pass-123";
    public static final String ADA = "ada@skillbridge.local";     // EMPLOYEE (seeded)
    public static final String TUNDE = "tunde@skillbridge.local"; // MANAGER (seeded) - verify in V3
    public static final String CHIDI = "chidi@skillbridge.local"; // ADMIN (seeded) - verify in V3

    private TestAuth() {
    }

    public static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    public static String tokenFor(MockMvc mvc, String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");
        return token;
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}