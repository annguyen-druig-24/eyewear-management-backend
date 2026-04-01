package com.swp391.eyewear_management_backend.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.swp391.eyewear_management_backend.dto.request.AuthenticationRequest;
import com.swp391.eyewear_management_backend.dto.request.IntrospectRequest;
import com.swp391.eyewear_management_backend.dto.request.LogoutRequest;
import com.swp391.eyewear_management_backend.dto.request.RefreshRequest;
import com.swp391.eyewear_management_backend.dto.response.AuthenticationResponse;
import com.swp391.eyewear_management_backend.dto.response.IntrospectResponse;
import com.swp391.eyewear_management_backend.dto.response.RoleResponse;
import com.swp391.eyewear_management_backend.entity.InvalidatedToken;
import com.swp391.eyewear_management_backend.entity.Role;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.repository.InvalidatedTokenRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

/*
    - Nghiệp vụ auth chính: login, introspect, logout, refresh, verify token, generate token.
*/

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepo userRepo;

    InvalidatedTokenRepo invalidatedTokenRepo;

    @NonFinal
    @Value("${jwt.signerKey}")  //Đọc biến signerKey từ application.yaml để lưu trữ kí, khi DevOps DEPLOY sẽ dùng signerKey khác
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    /*
        1) Mục đích: - Trả `valid=true/false` cho token.
        2) Steps:
            1. Lấy token từ request.
            2. Gọi `verifyToken(token, false)`.
            3. Nếu ném `AppException` thì đánh dấu invalid.
            4. Trả `IntrospectResponse`.
     */
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    /*
        1) Mục đích: - Đăng nhập và cấp JWT.
        2) Steps:
            1. Tìm user theo username.
            2. So khớp password plain với hash BCrypt.
            3. Sai mật khẩu → `UNAUTHENTICATED`.
            4. Đúng → `generateToken(user)`.
            5. Trả `AuthenticationResponse`.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepo.findByUsernameCaseSensitive(request.getUsername())      //phân biệt username hoa thường
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .role(extractRole(user))
                .name(user.getName())
                .build();
    }

    /*
        1) Mục đích: - Đăng xuất theo kiểu stateless JWT bằng blacklist.
        2) Steps:
            1. Verify token ở chế độ refresh window (`isRefresh=true`).
            2. Lấy `jti` + `exp` từ claims.
            3. Lưu vào `InvalidatedToken`.
            4. Nếu token đã hỏng/hết hạn thì log info (idempotent).
     */
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepo.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired");
        }
    }

    /*
        1) Mục đích: - Cấp token mới từ token cũ còn trong thời gian refresh.
        2) Steps:
            1. `verifyToken(oldToken, true)`.
            2. Blacklist old token theo `jti`.
            3. Lấy `subject` (username).
            4. Tải user từ DB.
            5. `generateToken(user)` mới.
            6. Trả response mới.
     */
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJwt = verifyToken(request.getToken(), true);

        var jit = signJwt.getJWTClaimsSet().getJWTID();
        var expiryTime = signJwt.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepo.save(invalidatedToken);

        var username = signJwt.getJWTClaimsSet().getSubject();

        var user = userRepo.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .role(extractRole(user))
                .name(user.getName())
                .build();
    }

    /*
        1) Mục đích: - Chuẩn hóa role string trả cho frontend (`trim + uppercase`).
     */
    private String extractRole(User user) {
        if (user == null || user.getRole() == null || user.getRole().getTypeName() == null) {
            return null;
        }

        return user.getRole().getTypeName().trim().toUpperCase();
    }

    /*
        1) Mục đích:
            - Verify chữ ký token bằng `SIGNER_KEY` (HS512).
            - Kiểm tra thời gian còn hợp lệ (2 mode: normal và refresh).
            - Kiểm tra token có bị blacklist chưa.
        2) Logic chi tiết:
            1. Parse raw token -> `SignedJWT`.
            2. Dùng `MACVerifier` + `SIGNER_KEY` để xác minh chữ ký.
            3. Xác định mốc thời gian hợp lệ tương ứng mode.
            4. Nếu signature fail hoặc quá hạn -> `UNAUTHENTICATED`.
            5. Check `invalidatedTokenRepo.existsById(jti)`:
               - có trong blacklist => reject.
            6. Trả về `SignedJWT` để caller tiếp tục dùng claims.
        3) Ý nghĩa bảo mật:
            - Chống giả mạo token (nhờ verify chữ ký).
            - Chặn token hết hạn.
            - Chặn token đã logout/refresh (replay token cũ).
     */
    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        //Kiem tra token da het han hay chua
        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if(!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if(invalidatedTokenRepo.existsById(signedJWT.getJWTClaimsSet().getJWTID())) throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    /*
        1) Mục đích: - Tạo JWT mới và ký HS512.
        2) Claims quan trọng:
            - `sub`: username.
            - `iss`: `eyewear-management.com`.
            - `iat`: thời điểm tạo.
            - `exp`: hết hạn theo `VALID_DURATION`.
            - `jti`: UUID chống replay/ phục vụ blacklist.
            - `scope`: danh sách role (VD `ROLE_ADMIN`).
     */
    //Build Token
    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("eyewear-management.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        //Sign Token
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    /*
        1) Mục đích: - Tạo chuỗi authority lưu vào claim `scope`.
        2) Logic:
            - Nếu user có role: trả `ROLE_` + `typeName` (uppercase).
            - Ví dụ DB role `sales staff` → claim `ROLE_SALES STAFF`.
     */
    private String buildScope(User user) {
        StringJoiner joiner = new StringJoiner(",");

        if (user != null && user.getRole() != null) {
            String roleType = user.getRole().getTypeName();
            if (roleType != null && !roleType.isBlank()) {
                joiner.add("ROLE_" + roleType.trim().toUpperCase());
            }
        }
        return joiner.toString();
    }

}
