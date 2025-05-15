package com.inventory.config;

import com.inventory.security.CustomUserDetailsService;
import com.inventory.service.LogService;
import com.inventory.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import com.inventory.model.User;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final LogService logService;
    private final CustomUserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(UserService userService, LogService logService, CustomUserDetailsService userDetailsService) {
        this.userService = userService;
        this.logService = logService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/images/**", "/api/init").permitAll()
                .requestMatchers("/login", "/users/register").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/admin/users/**").hasAnyRole("ADMIN", "DEMO")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "DEMO", "MANAGER")
                .requestMatchers("/sales/**").hasAnyRole("ADMIN", "DEMO", "MANAGER")
                .requestMatchers("/inventory/**").hasAnyRole("ADMIN", "DEMO", "MANAGER", "USER")
                .requestMatchers("/").hasAnyRole("ADMIN", "DEMO", "MANAGER", "USER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/inventory")
                .permitAll()
                .successHandler(loginSuccessHandler())
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .permitAll()
                .logoutSuccessHandler(logoutSuccessHandler())
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/admin/users/**")
            )
            .headers(headers -> headers.frameOptions().disable());
        
        return http.build();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setSameSite("Strict");
        return serializer;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
    
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            try {
                String ipAddress = getClientIp(request);
                
                // ユーザー情報を取得
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();
                
                User user = userService.findByUsername(username);
                
                // ログインログを記録
                logService.logAction("ログイン", "ユーザーがログインしました", user, ipAddress);
                
                // デフォルトのリダイレクト先へ
                response.sendRedirect("/inventory");
            } catch (Exception e) {
                System.err.println("ログイン処理中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace(); // スタックトレースを出力
                try {
                    response.sendRedirect("/login?error");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
    
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            try {
                if (authentication != null) {
                    String ipAddress = getClientIp(request);
                    
                    // ユーザー情報を取得
                    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                    String username = userDetails.getUsername();
                    
                    User user = userService.findByUsername(username);
                    
                    // ログアウトログを記録
                    logService.logAction("ログアウト", "ユーザーがログアウトしました", user, ipAddress);
                }
                
                // ログアウト後のリダイレクト先へ
                response.sendRedirect("/login?logout");
            } catch (Exception e) {
                // エラーログ出力
                System.err.println("ログアウト処理中にエラーが発生しました: " + e.getMessage());
                // エラー発生時も通常の遷移先へ
                try {
                    response.sendRedirect("/login?logout");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
} 