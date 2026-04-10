package com.mycompany.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mycompany.config.MapStructConfig;
import com.mycompany.dto.request.RegisterRequestDTO;
import com.mycompany.entity.UserEntity;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    // Fields mapped automatically: username, email, fullName
    // Fields ignored (set manually in service — require business logic):
    // password → must be encoded via PasswordEncoder
    // role → set from EnumRole constant
    // emailVerified → server-side flag, always false on registration
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    UserEntity toUserEntity(RegisterRequestDTO dto);
}
