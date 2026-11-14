package com.rainops.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfiguration {
    @Bean
    @Profile("!demo")
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable()).build();
    }

    @Bean
    @Profile("!demo")
    UserDetailsService localUsers() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    @Profile("demo")
    SecurityFilterChain demoSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/health", "/error", "/login-assets/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(Customizer.withDefaults())
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getServletPath().startsWith("/api/")))
                .build();
    }

    @Bean
    @Profile("demo")
    UserDetailsService demoUsers(@Value("${rainops.demo.username}") String username,
                                @Value("${rainops.demo.password}") String password) {
        if (password.isBlank() || password.length() < 8) {
            throw new IllegalArgumentException("DEMO_PASSWORD must contain at least 8 characters.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password))
                .roles("REVIEWER").build());
    }
}
