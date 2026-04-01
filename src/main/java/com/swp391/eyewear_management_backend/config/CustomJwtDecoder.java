package com.swp391.eyewear_management_backend.config;
import java.text.ParseException;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;

import com.swp391.eyewear_management_backend.dto.request.IntrospectRequest;
import com.swp391.eyewear_management_backend.service.impl.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;

/*
    - Decoder JWT tùy chỉnh.
    - Kiểm tra token qua `introspect()` trước, sau đó verify chữ ký HS512.
    --> Giải mã/kiểm tra token inbound
 */

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    /*
        1) Dùng để làm gì? --> Giải mã + xác thực token cho Spring Security khi request mang Bearer token
        2) Được dùng ở đâu? --> Được gọi tự động bởi OAuth2 Resource Server trong `SecurityConfig`
        3) Quy trình:
            1. Gọi `authenticationService.introspect(token)`.
            2. Nếu `valid=false` thì throw `JwtException`.
            3. Lazy-init `NimbusJwtDecoder` với `signerKey` HS512.
            4. `nimbusJwtDecoder.decode(token)` để parse claims cho security context.
        4) Ý nghĩa:
            - Kết hợp 2 lớp bảo vệ:
                + lớp nghiệp vụ (`introspect`: check hết hạn + blacklist),
                + lớp crypto (`Nimbus`: verify chữ ký).
     */
    @Override
    public Jwt decode(String token) throws JwtException {

        try {
            var response = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build());

            if (!response.isValid()) throw new JwtException("Token invalid");
        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage());
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}
