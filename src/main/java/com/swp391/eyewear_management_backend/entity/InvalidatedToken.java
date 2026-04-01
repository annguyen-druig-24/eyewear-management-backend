package com.swp391.eyewear_management_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

/*
    - Entity blacklist token theo `jti`.
    - Lưu `id` (= JWT `jti`) và `expiryTime`.
    - Dùng như blacklist để chặn token đã logout/refresh.
*/

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidatedToken {

    @Id
    String id;
    Date expiryTime;
}
