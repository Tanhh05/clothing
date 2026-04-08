package com.clothing.mapper;

import com.clothing.dto.response.UserResponse;
import com.clothing.entity.RoleEntity;
import com.clothing.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(UserEntity user);

    default String map(RoleEntity role) {
        return role.getName();
    }
}
