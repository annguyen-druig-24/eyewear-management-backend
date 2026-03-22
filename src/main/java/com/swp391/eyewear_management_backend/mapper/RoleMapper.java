package com.swp391.eyewear_management_backend.mapper;


import com.swp391.eyewear_management_backend.dto.request.RoleRequest;
import com.swp391.eyewear_management_backend.dto.response.RoleResponse;
import com.swp391.eyewear_management_backend.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(source = "name", target = "typeName")
    @Mapping(target = "roleID", ignore = true)
    @Mapping(target = "users", ignore = true)
    Role toRole(RoleRequest request);

    @Mapping(source = "typeName", target = "name")
    RoleResponse toRoleResponse(Role role);
}
