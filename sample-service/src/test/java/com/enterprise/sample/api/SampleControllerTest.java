package com.enterprise.sample.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SampleControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void whoami_echoes_gateway_propagated_headers() throws Exception {
        mvc.perform(get("/api/v1/sample/whoami")
                        .header("X-Auth-Subject", "alice")
                        .header("X-Auth-Roles", "[ROLE_USER]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("alice"))
                .andExpect(jsonPath("$.data.roles").value("[ROLE_USER]"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void create_order_returns_envelope_with_generated_id() throws Exception {
        mvc.perform(post("/api/v1/sample/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Auth-Subject", "alice")
                        .content("{\"product\":\"widget\",\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.product").value("widget"))
                .andExpect(jsonPath("$.data.quantity").value(3));
    }

    @Test
    void create_order_rejects_blank_product() throws Exception {
        mvc.perform(post("/api/v1/sample/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"\",\"quantity\":1}"))
                .andExpect(status().is4xxClientError());
    }
}
