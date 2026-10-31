package com.skillbridge.support;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAuth {

    public static final String PASSWORD = "Demo-Pass-123";
    public static final String ADA = "ada@skillbridge.local";     // EMPLOYEE (seeded)
    public static final String TUNDE = "tunde@skillbridge.local"; // MANAGER (seeded)
    public static final String CHIDI = "chidi@skillbridge.local"; // ADMIN (seeded)

    private static final String NEW_USER_PASSWORD = "Passw0rd-Test";

    private TestAuth() {
    }

    public static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    public static String tokenFor(MockMvc mvc, String email) throws Exception {
        return login(mvc, email, PASSWORD);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Registers a brand-new EMPLOYEE through the real API and returns its "Bearer ..." header value. */
    public static String newEmployeeBearer(MockMvc mvc) throws Exception {
        String email = "u-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\",\"email\":\"" + email
                                + "\",\"password\":\"" + NEW_USER_PASSWORD + "\"}"))
                .andExpect(status().is2xxSuccessful());
        return bearer(login(mvc, email, NEW_USER_PASSWORD));
    }

    /** Reads "$.id" from a response. */
    public static String idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        return id;
    }

    private static String login(MockMvc mvc, String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");
        return token;
    }
}