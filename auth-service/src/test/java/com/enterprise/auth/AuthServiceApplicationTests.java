package com.enterprise.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.auth.user.UserEntity;
import com.enterprise.auth.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Slice integration test that boots the full auth-service context against the in-memory H2
 * database, runs the Flyway migrations, and asserts the seed users are present.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

  @Autowired private UserRepository users;

  /** Sanity check: context loads + Flyway seeded the {@code admin} user. */
  @Test
  void contextLoadsAndAdminUserIsSeeded() {
    UserEntity admin = users.findByUsername("admin").orElseThrow();
    assertThat(admin.isEnabled()).isTrue();
    assertThat(admin.getRoles()).extracting("name").contains("ADMIN", "USER");
  }
}
