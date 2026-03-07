package com.swp391.eyewear_management_backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRespone {

    String username;
    String email;
    String phone;
    String name;
    LocalDate dob;
    String address;
    String idNumber;
    RoleResponse role;
    Boolean status;
    Integer provinceCode;
    String provinceName;
    Integer districtCode;
    String districtName;
    String wardCode;
    String wardName;
}


