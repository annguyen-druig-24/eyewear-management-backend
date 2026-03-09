package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.AdminCreateUserRequest;
import com.swp391.eyewear_management_backend.dto.request.AdminUpdateUserRequest;
import com.swp391.eyewear_management_backend.dto.request.UpdateDefaultAddressRequest;
import com.swp391.eyewear_management_backend.dto.request.UserCreationRequest;
import com.swp391.eyewear_management_backend.dto.request.UserUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.UserRespone;
import com.swp391.eyewear_management_backend.service.UserService;
import com.swp391.eyewear_management_backend.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    UserService userService;

    @PostMapping
    ApiResponse<UserRespone> createUser(@RequestBody @Valid UserCreationRequest request) {
        ApiResponse<UserRespone> apiRespone = new ApiResponse<>();

        apiRespone.setResult(userService.createRequest(request));

        return apiRespone;
    }
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping
    ApiResponse<List<UserRespone>> getUsers() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Username : {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info("Authority : {}", grantedAuthority.getAuthority()));

        return ApiResponse.<List<UserRespone>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserRespone> getUser(@PathVariable Long userId) {
        return ApiResponse.<UserRespone>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserRespone> getMyInfo() {
        return ApiResponse.<UserRespone>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping("/my-info")
    ApiResponse<UserRespone> updateMyInfo(@RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserRespone>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

//    @PutMapping("/{userId}")
//    UserRespone updateUser(@PathVariable Long userId, @RequestBody @Valid UserUpdateRequest request) {
//        return userServiceImpl.updateUser(userId, request);
//    }
@PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')") // Chặn ngay từ Controller cho an toàn
    @DeleteMapping("/{userId}")
    public String deleteUserById(@PathVariable Long userId) {
        userService.deleteUserById(userId);
        return "User has been deleted";
    }

    @PutMapping("/my-address")
    ApiResponse<UserRespone> updateMyDefaultAddress(@RequestBody @Valid UpdateDefaultAddressRequest request) {
        return ApiResponse.<UserRespone>builder()
                .message("Update default address successfully")
                .result(userService.updateMyDefaultAddress(request))
                .build();
    }

    @PutMapping("/admin/update")
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")// Chặn ngay từ Controller cho an toàn
    public ApiResponse<UserRespone> updateUserByAdmin(
            @RequestBody @Valid AdminUpdateUserRequest request) {

        return ApiResponse.<UserRespone>builder()
                .message("Updated user successfully")
                .result(userService.updateUserByAdmin(request))
                .build();
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<UserRespone> createUserByAdmin(
            @RequestBody @Valid AdminCreateUserRequest request) {

        return ApiResponse.<UserRespone>builder()
                .message("User created successfully by admin")
                .result(userService.createUserByAdmin(request))
                .build();
    }

}
