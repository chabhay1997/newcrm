package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LeadPermissionWebConfig implements WebMvcConfigurer {

    private final LeadPermissionInterceptor leadPermissionInterceptor;

    public LeadPermissionWebConfig(LeadPermissionInterceptor leadPermissionInterceptor) {
        this.leadPermissionInterceptor = leadPermissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(leadPermissionInterceptor);
    }
}
