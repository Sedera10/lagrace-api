package mg.lagrace.api.auth.application;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    private final BookingPermissionService permissions;

    public PermissionInterceptor(BookingPermissionService permissions) { this.permissions = permissions; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        RequiresPermissions required = method.getMethodAnnotation(RequiresPermissions.class);
        if (required == null) return true;
        permissions.require(SecurityContextHolder.getContext().getAuthentication(), required.value());
        return true;
    }
}
