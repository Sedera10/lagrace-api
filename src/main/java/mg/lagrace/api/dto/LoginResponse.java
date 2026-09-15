package mg.lagrace.api.dto;

public record LoginResponse(String token, UserResponse user) {
}