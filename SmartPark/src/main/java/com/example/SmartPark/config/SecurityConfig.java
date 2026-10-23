package com.example.SmartPark.config;

import com.example.SmartPark.service.SmartParkUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {
    
    //?: Cấu hình bảo mật cho ứng dụng SmartPark.
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        return http
                 //?: đăng ký cơ chế xác thực tài khoản người dùng với Spring Security.
                .authenticationProvider(authenticationProvider)
                //?: Quy định quyền truy cập và phân quyền.
                .authorizeHttpRequests(authorize -> authorize

                        //!: Cho phép truy cập công khai vào các trang và tài nguyên tĩnh.
                        .requestMatchers(
                                "/", "/login", "/register", "/access-denied",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        //!: chỉ cho phép ADMIN được truy cập.
                        .requestMatchers("/admin/**", "/parking-sessions/**")
                        .hasRole("ADMIN") //!: -> phần quyền ADMIN
                        .requestMatchers("/vehicles/**")
                        .hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/user/**", "/reservations/**") //!: các chức năng người dùng và đặt chỗ.
                        .hasAnyRole("USER", "ADMIN") 
                        .anyRequest() //!: -> tất cả URL khác yêu cầu đăng nhập
                        .authenticated())
                //?: cấu hình đăng nhập form và xử lý thành công/ thất bại.
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler) //!: -> xử lý khi đăng nhập thành công 
                        .failureUrl("/login?error") 
                        .permitAll())
                //?: cấu hình đăng xuất 
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") //!: đăng xuất được -> chuyển hướng về đăng nhập 
                        .invalidateHttpSession(true) 
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied"))
                .build();
    }

    @Bean
    //!: tìm tài khoản và kiểm tra thông tin đăng nhập.
    DaoAuthenticationProvider authenticationProvider(SmartParkUserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    //?: khai báo mã hóa mật khẩu 
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //?: xử lý điều hướng sau khi đăng nhập thành công 
    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
                //!: kiểm tra role admin hay user.
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(admin ? "/admin/dashboard" : "/user/parking"); //!: tất cả chuyển sang home
        };
    }
}
