package com.enterprise.auth.demo;

import java.util.List;

/**
 * Plain domain representation of a user (stand-in for a JPA entity in this
 * demo). Mapped to {@link UserDto} via {@link UserMapper}.
 *
 * @param id            internal identifier
 * @param username      login name
 * @param passwordHash  encoded password (never serialized to the DTO)
 * @param roles         granted roles
 */
public record UserEntity(Long id, String username, String passwordHash, List<String> roles) {}
