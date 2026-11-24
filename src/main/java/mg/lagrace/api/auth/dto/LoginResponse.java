package mg.lagrace.api.auth.dto;

import mg.lagrace.api.auth.User;

import mg.lagrace.api.auth.dto.user.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}