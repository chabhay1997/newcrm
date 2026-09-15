package config;

import service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;


    // Constructor Injection
    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService) {

        this.customUserDetailsService = customUserDetailsService;
    }


    // =========================================================
    // AUTHENTICATION PROVIDER
    // =========================================================

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(
                customUserDetailsService
        );

        authProvider.setPasswordEncoder(
                passwordEncoder
        );

        return authProvider;
    }


    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig)
            throws Exception {

        return authConfig.getAuthenticationManager();
    }


    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http)
            throws Exception {

        http

            // -------------------------------------------------
            // URL AUTHORIZATION
            // -------------------------------------------------

            .authorizeHttpRequests(auth -> auth

                // Public URLs
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/evtl.webp",
                    "/*.webp",
                    "/favicon.ico"
                )
                .permitAll()

                // All other URLs require login
                .anyRequest()
                .authenticated()
            )

            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
            )

            // -------------------------------------------------
            // LOGIN
            // -------------------------------------------------

            .formLogin(form -> form

                // Login page
                .loginPage("/login")

                // Form POST URL
                .loginProcessingUrl("/login")

                // Successful login
                .defaultSuccessUrl(
                        "/dashboard",
                        true
                )

                // Failed login
                .failureUrl(
                        "/login?error=true"
                )

                .permitAll()
            )


            // -------------------------------------------------
            // LOGOUT
            // -------------------------------------------------

            .logout(logout -> logout

                .logoutUrl("/logout")

                .logoutSuccessUrl(
                        "/?logout=true"
                )

                .invalidateHttpSession(true)

                .clearAuthentication(true)

                .deleteCookies(
                        "JSESSIONID"
                )

                .permitAll()
            );


        return http.build();
    }
}
