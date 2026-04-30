package com.enterprise.auth.service;

import com.enterprise.auth.api.LoginRequest;
import com.enterprise.auth.api.TokenResponse;
import com.enterprise.auth.token.RefreshTokenService;
import com.enterprise.auth.user.LoginAttemptService;
import com.enterprise.common.config.CommonProperties;
import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.security.JwtTokenProvider;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokens;
    private final LoginAttemptService loginAttempts;
    private final CommonProperties commonProps;

    public AuthService(AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenService refreshTokens,
                       LoginAttemptService loginAttempts,
                       CommonProperties commonProps) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tokenProvider = tokenProvider;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
        this.commonProps = commonProps;
    }

    @RateLimiter(name = "auth-login", fallbackMethod = "loginFallback")
    public TokenResponse login(LoginRequest req, HttpServletRequest http) {
        loginAttempts.assertNotLocked(req.username());
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
            String access = tokenProvider.generateAccessToken(auth.getName(), roles, Map.of());
            String refresh = refreshTokens.issue(auth.getName(), http);
            loginAttempts.onSuccess(auth.getName());
            return new TokenResponse(access, refresh, "Bearer",
                    commonProps.jwt().accessTokenTtl().toSeconds());
        } catch (BadCredentialsException ex) {
            loginAttempts.onFailure(req.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }
    }

    @SuppressWarnings("unused")
    private TokenResponse loginFallback(LoginRequest req, HttpServletRequest http, Throwable t) {
        if (t instanceof BusinessException be) throw be;
        throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                "Too many login attempts, please retry later");
    }

    public TokenResponse refresh(String refreshToken, HttpServletRequest http) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(refreshToken, http);
        UserDetails user = userDetailsService.loadUserByUsername(rotation.username());
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        String access = tokenProvider.generateAccessToken(user.getUsername(), roles, Map.of());
        return new TokenResponse(access, rotation.rawRefreshToken(), "Bearer",
                commonProps.jwt().accessTokenTtl().toSeconds());
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    public void logoutAll(String username) {
        refreshTokens.revokeAll(username);
    }
}