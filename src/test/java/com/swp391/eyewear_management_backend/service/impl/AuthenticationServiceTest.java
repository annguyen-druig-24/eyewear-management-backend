package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.AuthenticationRequest;
import com.swp391.eyewear_management_backend.entity.Role;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.repository.InvalidatedTokenRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final String SIGNER_KEY = "56235e27daf8f8e68ce33febf5400cf93dbd5f6ec40a14531754168ea0d32486";

    @Mock
    UserRepo userRepo;

    @Mock
    InvalidatedTokenRepo invalidatedTokenRepo;

    @InjectMocks
    AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", SIGNER_KEY);
        ReflectionTestUtils.setField(authenticationService, "VALID_DURATION", 3600L);
        ReflectionTestUtils.setField(authenticationService, "REFRESHABLE_DURATION", 36000L);
    }

    @Test
    void authenticate_whenUsernameMatchesExactCase_shouldLoginSuccessfully() {
        String rawPassword = "Password123";
        User user = buildUser("leducloi", rawPassword);
        when(userRepo.findByUsernameCaseSensitive("leducloi")).thenReturn(Optional.of(user));

        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("leducloi")
                .password(rawPassword)
                .build();

        var response = authenticationService.authenticate(request);

        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
        verify(userRepo).findByUsernameCaseSensitive("leducloi");
    }

    @Test
    void authenticate_whenUsernameCaseIsDifferent_shouldRejectLogin() {
        when(userRepo.findByUsernameCaseSensitive("LEDUCLOI")).thenReturn(Optional.empty());

        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("LEDUCLOI")
                .password("Password123")
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);

        verify(userRepo).findByUsernameCaseSensitive("LEDUCLOI");
        verify(userRepo, never()).findByUsername("LEDUCLOI");
    }

    private User buildUser(String username, String rawPassword) {
        Role role = new Role();
        role.setTypeName("CUSTOMER");

        User user = new User();
        user.setUsername(username);
        user.setPassword(new BCryptPasswordEncoder(10).encode(rawPassword));
        user.setRole(role);
        user.setName("Le Duc Loi");
        return user;
    }
}
