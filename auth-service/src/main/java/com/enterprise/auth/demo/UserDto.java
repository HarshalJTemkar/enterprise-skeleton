package com.enterprise.auth.demo;

import java.util.List;

/** Transport object exposed over the REST API. Deliberately excludes the password hash. */
public record UserDto(Long id, String username, List<String> roles) {}
