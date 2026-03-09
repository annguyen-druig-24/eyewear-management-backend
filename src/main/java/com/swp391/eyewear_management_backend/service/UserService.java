package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.AdminCreateUserRequest;
import com.swp391.eyewear_management_backend.dto.request.AdminUpdateUserRequest;
import com.swp391.eyewear_management_backend.dto.request.UpdateDefaultAddressRequest;
import com.swp391.eyewear_management_backend.dto.request.UserCreationRequest;
import com.swp391.eyewear_management_backend.dto.request.UserUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.UserRespone;
import com.swp391.eyewear_management_backend.entity.User;

import java.util.List;

public interface UserService {

    // CREATE
    UserRespone createRequest(UserCreationRequest request);

    // ADMIN - CREATE user (với tất cả các thông tin)
    UserRespone createUserByAdmin(AdminCreateUserRequest request);

    // READ - thông tin user đang đăng nhập
    UserRespone getMyInfo();

    // UPDATE - thông tin user đang đăng nhập
    UserRespone updateMyInfo(UserUpdateRequest request);

    // ADMIN - list users
    List<UserRespone> getUsers();

    // READ by id
    UserRespone getUserById(Long id);

    // DELETE
    void deleteUserById(Long id);

    // (Optional) nếu bạn vẫn muốn giữ method này
    User findByName(String userName);

    UserRespone updateMyDefaultAddress(UpdateDefaultAddressRequest request);

    public UserRespone updateUserByAdmin(AdminUpdateUserRequest request);
}