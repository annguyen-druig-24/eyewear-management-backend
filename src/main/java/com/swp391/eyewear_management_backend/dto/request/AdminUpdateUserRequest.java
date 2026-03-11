package com.swp391.eyewear_management_backend.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUpdateUserRequest {
    Long id;
    String name;
    String phone;
    String address;
    Boolean status;    // Dành cho việc đổi trạng thái (Active/Inactive)
    String roleName;   // Dành cho việc cấp quyền (ADMIN, SALES STAFF, CUSTOMER...)
}
