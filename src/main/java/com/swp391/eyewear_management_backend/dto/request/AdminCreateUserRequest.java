package com.swp391.eyewear_management_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCreateUserRequest {

    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 8, message = "USERNAME_INVALID")
    String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "PASSWORD_INVALID"
    )
    String password;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    String email;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^\\d{10,11}$", message = "PHONE_INVALID")
    String phone;

    @NotBlank(message = "NAME_REQUIRED")
    String name;

    @Past(message = "DOB_INVALID")
    LocalDate dob;

    String address;

    @Pattern(regexp = "^\\d{12}$", message = "IDNUMBER_INVALID")
    String idNumber;

    // Admin có thể set status ngay từ đầu (default = true nếu không truyền)
    Boolean status;

    // Admin có thể chỉ định role ngay khi tạo user
    @NotBlank(message = "ROLE_REQUIRED")
    String roleName;  // VD: CUSTOMER, SALES_STAFF, OPERATION_STAFF, ADMIN

    // GHN địa chỉ (optional)
    Integer provinceCode;

    String provinceName;

    Integer districtCode;

    String districtName;

    String wardCode;

    String wardName;
}
