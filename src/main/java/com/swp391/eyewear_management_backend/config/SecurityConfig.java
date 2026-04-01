package com.swp391.eyewear_management_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
    - Cấu hình Spring Security filter chain.
    - Public endpoint, protected endpoint, OAuth2 Resource Server (JWT), CORS, CSRF.
    - Mapping claim `scope` trong JWT thành authorities để dùng `@PreAuthorize`.
    --> Cấu hình bảo mật tổng
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsProperties corsProperties;

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/payments/vnpay/**",
            "/ghn/**",
            "/api/products/search",
            "/api/products/**",   // lưu ý: /{id} nên dùng /** thay vì {id}
            "/api/v1/dashboard/top-products",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"

    };

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/users",
            "/auth/token",
            "/auth/introspect",
            "/auth/logout",
            "/auth/refresh",
            "/api/payment/payos-webhook",
            "/api/prescriptions/parse-image",
            "/api/chatbot/recommend"
    };

    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    /*
        1) Dùng để làm gì? --> Xây dựng toàn bộ pipeline security cho mọi request.
        2) Được dùng ở đâu? --> Spring Boot tự gọi vì đây là bean
        3) Quy trình hoạt động:
            1. Bật CORS.
            2. Khai báo endpoint public theo method (GET/POST).
            3. Khai báo một số endpoint cần role cụ thể.
            4. Các endpoint còn lại buộc authenticated.
            5. Cấu hình JWT resource server + custom decoder.
            6. Gắn handler cho 401/403.
            7. Disable CSRF (phù hợp API stateless dùng JWT).
        4) Logic xử lý chính:
            - requestMatchers(...).permitAll() → cho qua không cần token.
            - hasAnyAuthority(...) → yêu cầu role cụ thể.
            - .anyRequest().authenticated() → mặc định phải đăng nhập.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.cors(Customizer.withDefaults());

        httpSecurity.authorizeHttpRequests(req -> req
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                .requestMatchers("/api/staff/orders/**").hasAnyAuthority("ROLE_SALES STAFF", "ROLE_ADMIN", "ROLE_MANAGER")
                .requestMatchers("/api/inventory/**").hasAnyAuthority("ROLE_OPERATIONS STAFF", "ROLE_SALES STAFF", "ROLE_ADMIN", "ROLE_MANAGER")
                .anyRequest().authenticated()
        );

        //Config cho oauth2 để verify token
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer ->     //nói với Spring là access token dạng JWT.
                                jwtConfigurer.decoder(customJwtDecoder)     //gọi customJwtDecoder để decode(token)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))      //map claim trong JWT thành quyền trong Spring Security.
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())    //Dùng để handle Error 401 --> Chuẩn hóa JSON lỗi khi chưa xác thực.
        );

        httpSecurity.exceptionHandling(ex -> ex
                .accessDeniedHandler(new JwtAccessDeniedHandler())  //Dùng để handle Error 403
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable);     //disable csrf

        return httpSecurity.build();
    }

    /*
        ### Giải thích sâu về oauth2:
        1) Ý nghĩa tổng quan:
            - Cấu hình ứng dụng theo vai trò **OAuth2 Resource Server**
            --> Nghĩa là backend này **không login theo session**, mà nhận Bearer token (JWT) từ client, rồi tự xác thực token đó trước khi cho truy cập API protected.
        2) Luồng chạy khi có request `Authorization: Bearer <token>`:
            1. Security filter đọc header Bearer token.
            2. Đi vào nhánh `oauth2ResourceServer().jwt(...)`.
            3. Gọi `customJwtDecoder.decode(token)` để verify + parse claims.
            4. Nếu decode thành công, Spring tạo `Authentication` object.
            5. `jwtAuthenticationConverter()` chuyển claims (đặc biệt claim `scope`) thành `GrantedAuthority`.
            6. Các rule `.hasAnyAuthority(...)` và `@PreAuthorize(...)` dùng authorities đó để quyết định cho qua/chặn.
            7. Nếu lỗi xác thực (token sai/hết hạn/không có token khi endpoint yêu cầu auth) thì trả lỗi qua `JwtAuthenticationEntryPoint` (401).
         ==> Đoạn oauth2 trong SecurityConfig này là “cổng kiểm tra token” của toàn hệ thống.
     */

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        // React dev server
//        config.setAllowedOrigins(List.of("http://localhost:3000"));
//        // hoặc nếu muốn mọi origin:
//        // config.addAllowedOriginPattern("*");
//
//        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
//        config.setAllowedHeaders(List.of("*"));
//        config.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }

//    @Bean
//    public CorsFilter corsFilter() {
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//
//        corsConfiguration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
//        corsConfiguration.addAllowedHeader("*");
//        corsConfiguration.addAllowedMethod("*");
//        corsConfiguration.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfiguration);
//
//        return new CorsFilter(source);

    /*
        1) Dùng để làm gì? --> Cho phép frontend domain hợp lệ gọi API
        2) Được dùng ở đâu? --> Được Security filter chain dùng khi bật CORS
        3) Các step:
            1. Đọc `allowedOriginPatterns` từ `CorsProperties`.
            2. Cho phép methods/headers cần thiết.
            3. Bật credentials.
            4. Register áp dụng cho `/**`.
        4) Diễn giải dễ hình dung:
            - Frontend domain hợp lệ gọi API backend.
            - Browser gửi preflight OPTIONS (khi cần).
            - Backend trả về “policy CORS”: origin nào được phép, method/header nào được phép, có cho credentials hay không.
            - Browser dựa vào policy đó quyết định cho JS frontend đọc response hay chặn.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());              //Xác định các origin (domain frontend) được phép gọi API.
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.addAllowedHeader("*");        //Cho phép client gửi mọi HTTP header trong CORS request (vd: `Authorization`, `Content-Type`, `X-...` custom header).
        corsConfiguration.addAllowedMethod("*");        //Cho phép mọi HTTP method.
        corsConfiguration.setAllowCredentials(true);    //Cho phép gửi thông tin credentials trong cross-origin request: cookies, Authorization header, TLS client certificate.
        corsConfiguration.setMaxAge(3600L);     //Thời gian (giây) browser cache kết quả preflight (`OPTIONS`) --> giảm số lần browser phải gửi preflight lặp lại, cải thiện hiệu năng

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    /*
        1) Dùng để làm gì? --> Map claim `scope` từ JWT thành `GrantedAuthority` để `@PreAuthorize` hiểu được
        2) Được dùng ở đâu? --> Trong `oauth2ResourceServer().jwt().jwtAuthenticationConverter(...)`
        3) Logic:
            - `setAuthorityPrefix("")`: không thêm `SCOPE_` mặc định.
            - `setAuthoritiesClaimDelimiter(",")`: tách nhiều authority theo dấu phẩy.
            - Vì token build dạng `ROLE_ADMIN,ROLE_MANAGER` nên rất phù hợp.
    */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimDelimiter(",");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

//    @Bean
//    JwtDecoder jwtDecoder() {
//        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
//
//        return NimbusJwtDecoder
//                .withSecretKey(secretKeySpec)
//                .macAlgorithm(MacAlgorithm.HS512)
//                .build();
//    };

    /*
        1) Dùng để làm gì? --> Cấp bean BCrypt encoder (strength 10)
        2) Được dùng ở đâu? --> Service tạo/cập nhật mật khẩu có thể inject bean này
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

}
