package com.example.demo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager aus der Spring-Config ziehen (nimmt unseren Provider)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(encoder);
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authManager,
                                           JwtUtil jwtUtil) throws Exception {

        // Unser Login-Filter (liest JSON, authentifiziert, erzeugt JWT)
        JwtAuthenticationFilter loginFilter = new JwtAuthenticationFilter(authManager, jwtUtil);
        loginFilter.setFilterProcessesUrl("/login"); // exakt dieser Pfad wird vom Test aufgerufen

        // Unser JWT-Auth-Filter für alle anderen Requests
        JwtAuthorizationFilter jwtAuthzFilter = new JwtAuthorizationFilter(jwtUtil);

        http
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // WICHTIG: Ohne Token -> 401 (nicht 403)
                        .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        // Mit Token aber ohne Rechte -> 403
                        .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/create").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/item/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider(null, null)); // wird via Spring ersetzt

        // Filter-Reihenfolge:
        // 1) Login-Filter genau an Position des UsernamePasswordAuthenticationFilter
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
        // 2) JWT-Authorization-Filter vor dem UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthzFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
