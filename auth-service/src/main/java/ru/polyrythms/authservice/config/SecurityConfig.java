package ru.polyrythms.authservice.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.polyrythms.authservice.filter.InternalAuthFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilterRegistration(InternalAuthFilter filter) {
        FilterRegistrationBean<InternalAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(1);
        return registration;
    }
}