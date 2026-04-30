package com.enterprise.auth.controller;

import com.enterprise.auth.api.LoginRequest;
import com.enterprise.auth.api.LogoutRequest;
import com.enterprise.auth.api.RefreshRequest;
import com.enterprise.auth.api.TokenResponse;
import com.enterprise.auth.service.AuthService;
import com.enterprise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT tokens (rate-limited, lockout after N failures)")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req,
                                            HttpServletRequest http) {
        return ApiResponse.ok(authService.login(req, http));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token: revokes the supplied token and issues a new pair")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req,
                                              HttpServletRequest http) {
        return ApiResponse.ok(authService.refresh(req.refreshToken(), http));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a single refresh token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req.refreshToken());
    }

    @PostMapping("/logout/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke ALL refresh tokens for the authenticated principal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal UserDetails principal) {
        authService.logoutAll(principal.getUsername());
    }
}