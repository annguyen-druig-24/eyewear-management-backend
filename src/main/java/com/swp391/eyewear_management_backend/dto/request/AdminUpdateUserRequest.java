package com.swp391.eyewear_management_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUpdateUserRequest {
    String username;

    @Email(message = "EMAIL_INVALID")
    @Pattern(regexp = "^(?!\\s*$).+", message = "EMAIL_INVALID")
    String email;

    String name;
    String phone;
    String address;
    Boolean status;    // Dành cho việc đổi trạng thái (Active/Inactive)
    String roleName;   // Dành cho việc cấp quyền (ADMIN, SALES STAFF, CUSTOMER...)
}
