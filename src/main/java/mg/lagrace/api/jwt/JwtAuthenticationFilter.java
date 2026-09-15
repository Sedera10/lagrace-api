package mg.lagrace.api.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.lagrace.api.services.AuthService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final AuthService authService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService
            userDetailsService, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    String tokenId = jwtUtil.extractTokenId(token);
                    if(!authService.isRevoked(tokenId)){
                        String username = jwtUtil.extractUsername(token);
                        if (username != null &&
                                SecurityContextHolder.getContext().getAuthentication() == null) {
                            UserDetails userDetails =
                                    userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(userDetails, null,
                                            userDetails.getAuthorities());

                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    } else {
                        request.setAttribute("jwt.error", "TOKEN_REVOKED");
                    }
                } else {
                    request.setAttribute("jwt.error", "TOKEN_INVALID_OR_EXPIRED");
                }
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                request.setAttribute("jwt.error", "TOKEN_INVALID_OR_EXPIRED");
            }
        }
        filterChain.doFilter(request, response);
    }
}