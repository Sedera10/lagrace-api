package mg.lagrace.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import mg.lagrace.api.auth.application.PermissionInterceptor;

@Configuration
public class PermissionWebConfig implements WebMvcConfigurer {
    private final PermissionInterceptor interceptor;
    public PermissionWebConfig(PermissionInterceptor interceptor) { this.interceptor = interceptor; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor); }
}
