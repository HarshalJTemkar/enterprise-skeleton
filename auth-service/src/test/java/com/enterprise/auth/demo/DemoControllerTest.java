package com.enterprise.auth.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.common.test.WithMockJwtUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end test of the {@code /demo/mapper} endpoint. Demonstrates the use of the shared {@link
 * WithMockJwtUser} fixture from {@code common-lib}.
 */
@SpringBootTest
@ActiveProfiles("test")
class DemoControllerTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  /** MockMvc setup using the full Spring Security filter chain. */
  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(
                org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                    .springSecurity())
            .build();
  }

  /** Demo endpoints are open – this just verifies routing & MapStruct work. */
  @Test
  @WithMockJwtUser(
      username = "alice",
      roles = {"USER"})
  void mapperReturnsDtoWithoutPasswordHash() throws Exception {
    mockMvc
        .perform(get("/demo/mapper"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value("demo"))
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
  }
}
