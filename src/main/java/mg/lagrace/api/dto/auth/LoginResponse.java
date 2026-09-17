package mg.lagrace.api.dto.auth;

import mg.lagrace.api.dto.user.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}