package com.enterprise.auth.user;

import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks failed-login attempts per username and locks the account for
 * {@code auth.lockout.duration} after {@code auth.lockout.max-attempts}
 * consecutive failures.
 */
@Service
public class LoginAttemptService {

    private final UserRepository users;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(UserRepository users,
                               @Value("${auth.lockout.max-attempts:5}") int maxAttempts,
                               @Value("${auth.lockout.duration:PT15M}") Duration lockDuration) {
        this.users = users;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    @Transactional(readOnly = true)
    public void assertNotLocked(String username) {
        users.findByUsername(username).ifPresent(u -> {
            if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now())) {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                        "Account temporarily locked. Try again later.");
            }
        });
    }

    @Transactional
    public void onFailure(String username) {
        users.findByUsername(username).ifPresent(u -> {
            int attempts = u.getFailedLoginAttempts() + 1;
            u.setFailedLoginAttempts(attempts);
            if (attempts >= maxAttempts) {
                u.setLockedUntil(Instant.now().plus(lockDuration));
                u.setFailedLoginAttempts(0);
            }
        });
    }

    @Transactional
    public void onSuccess(String username) {
        users.findByUsername(username).ifPresent(u -> {
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            u.setLastLoginAt(Instant.now());
        });
    }
}
