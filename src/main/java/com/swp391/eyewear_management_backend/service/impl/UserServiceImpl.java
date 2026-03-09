package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.AdminUpdateUserRequest;
import com.swp391.eyewear_management_backend.dto.request.UpdateDefaultAddressRequest;
import com.swp391.eyewear_management_backend.dto.request.UserCreationRequest;
import com.swp391.eyewear_management_backend.dto.request.UserUpdateRequest;
import com.swp391.eyewear_management_backend.dto.response.UserRespone;
import com.swp391.eyewear_management_backend.entity.Role;
import com.swp391.eyewear_management_backend.entity.User;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.mapper.UserMapper;
import com.swp391.eyewear_management_backend.repository.RoleRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import com.swp391.eyewear_management_backend.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor        //Tiêm Bean vào Class bằng CTOR -> Khỏi @Autowired
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)   //để kiểu private final tự động cho các field cần tiêm Bean vào class
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepo userRepo;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepo roleRepo;

    public UserRespone createRequest(UserCreationRequest request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        //Optional - Có thể nhập hoặc để trống
        String address = request.getAddress() != null ? request.getAddress().trim() : null;
        String idNumber = request.getIdNumber() != null ? request.getIdNumber().trim() : null;

        if(userRepo.existsByUsername(request.getUsername())) throw new AppException(ErrorCode.USER_EXISTED);
        if(userRepo.existsByEmail(request.getEmail())) throw new AppException(ErrorCode.EMAIL_EXISTED);
        if (idNumber != null && userRepo.existsByIdNumber(idNumber)) {
            throw new AppException(ErrorCode.IDNUMBER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setUsername(username);
        user.setEmail(email);
        user.setAddress(address);
        user.setIdNumber(idNumber);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(true);

        Role role = roleRepo.findByTypeName("CUSTOMER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);

        return userMapper.toUserRespone(userRepo.save(user));
    }

    public UserRespone getMyInfo() {
        var context = SecurityContextHolder.getContext();       //get User hiện tại
        String name = context.getAuthentication().getName();    //lấy ra name của user đang request

        User user = userRepo.findByUsername(name).orElseThrow(() ->
                new AppException(ErrorCode.USER_NOT_EXISTED));          //Kiểm tra xem có ko, nếu có thì hiển thị, nếu không thì throw Exception

        return userMapper.toUserRespone(user);
    }

    public UserRespone updateMyInfo(UserUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepo.findByUsername(username).orElseThrow(() ->
                new AppException(ErrorCode.USER_NOT_EXISTED));

        String newEmail = request.getEmail() != null ? request.getEmail().trim() : null;
        String newIdNumber = request.getIdNumber() != null ? request.getIdNumber().trim() : null;

        // Check unique nếu user có gửi email/idNumber
        if (newEmail != null && userRepo.existsByEmailAndUserIdNot(newEmail, user.getUserId())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        if (newIdNumber != null && userRepo.existsByIdNumberAndUserIdNot(newIdNumber, user.getUserId())) {
            throw new AppException(ErrorCode.IDNUMBER_EXISTED);
        }

        userMapper.updateUser(user, request);

        return userMapper.toUserRespone(userRepo.save(user));
    }

    //Phân quyền dựa trên Method
    @PreAuthorize("hasRole('ADMIN')")   //@PreAuthorize("hasRole('X')") sẽ chặn các user mà có role ko trùng với role X     --> Thỏa Method mới đc vào method
    //@PreAuthorize("hasAuthority('UPDATE_POST')")
    public List<UserRespone> getUsers() {
        log.info("In method getUsers");
        return userRepo.findAll().stream().map(userMapper::toUserRespone).toList();
    }

    @PostAuthorize("returnObject.username == authentication.name")  //@PostAuthorize("hasRole('Y')") sẽ cho phép user chạy hàm này, nhưng sau khi chạy xong sẽ kiểm tra, và nếu user có role ko trùng với Y thì sẽ bị chặn và ko hiển thị kết quả --> Sau khi thực hiện xong, nếu thỏa thì mới đc sử dụng (hiển thị kết quả), nếu ko thì chặn
    //@PostAuthorize sẽ được dùng khi để 1 user chỉ được xem thông tin của chính mình, ko xem được bất kì thông tin nào của người khác
    public UserRespone getUserById(Long id) {
        log.info("In method getUserById");
        return userMapper.toUserRespone(userRepo.findById(id).orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_EXISTED.getMessage())));
    }

//    public UserRespone updateUser(Long userId, UserUpdateRequest request) {
//        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_EXISTED.getMessage()));
//        userMapper.updateUser(user, request);
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//
//
//        return userMapper.toUserRespone(userRepo.save(user));
//    }

    public void deleteUserById(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setStatus(false);
        userRepo.save(user);
    }

    @Override
    public User findByName(String userName) {
        return userRepo.findByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    //hàm normalize để convert "" --> null , check trimOrNull
    private String normalize(String s) {
        if(s == null) return null;
        String exist = s.trim();
        return exist.isEmpty() ? s : exist;
    }

    @Override
    public UserRespone updateMyDefaultAddress(UpdateDefaultAddressRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String fullAddress = buildFullAddress(request);
        user.setAddress(fullAddress);
        // ✅ update code/name để lần sau khỏi cần chọn lại
        user.setProvinceCode(request.getProvinceCode());
        user.setProvinceName(request.getProvinceName());
        user.setDistrictCode(request.getDistrictCode());
        user.setDistrictName(request.getDistrictName());
        user.setWardCode(request.getWardCode());
        user.setWardName(request.getWardName());
        return userMapper.toUserRespone(userRepo.save(user));
    }

    private String buildFullAddress(UpdateDefaultAddressRequest request) {
        String street = request.getStreet().trim();
        String ward = request.getWardName().trim();
        String district = request.getDistrictName().trim();
        String province = request.getProvinceName().trim();

        return String.format("%s, %s, %s, %s", street, ward, district, province);
    }

    // ... các import cũ

    @Override
    @PreAuthorize("hasRole('ADMIN')") // Bắt buộc phải là ADMIN mới chạy được hàm này
    public UserRespone updateUserByAdmin(AdminUpdateUserRequest request) {
        log.info("Admin is updating user with ID: {}", request.getId());

        // 1. Tìm user cần update
        User user = userRepo.findById(request.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Cập nhật các trường cơ bản (nếu admin có truyền vào)
        if(request.getName() != null) user.setName(request.getName().trim());
        if(request.getPhone() != null) user.setPhone(request.getPhone().trim());
        if(request.getAddress() != null) user.setAddress(request.getAddress().trim());

        // 3. Cập nhật Status (Trạng thái làm việc)
        if(request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // 4. Cập nhật Role (Vai trò)
        if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
            Role role = roleRepo.findByTypeName(request.getRoleName().trim().toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            user.setRole(role);
        }

        // 5. Lưu xuống DB và trả về response
        return userMapper.toUserRespone(userRepo.save(user));
    }
}
