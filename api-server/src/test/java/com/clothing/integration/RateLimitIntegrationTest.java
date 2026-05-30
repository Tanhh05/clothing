package com.clothing.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.login.max-requests=2",
        "app.security.rate-limit.login.window-seconds=60",
        "app.security.rate-limit.register.max-requests=2",
        "app.security.rate-limit.register.window-seconds=60",
        "app.security.rate-limit.order-create.max-requests=2",
        "app.security.rate-limit.order-create.window-seconds=60"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn429WhenLoginRateLimitExceeded() throws Exception {
        String payload = "{\"email\":\"x@example.com\",\"password\":\"invalid\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests());
    }
}
