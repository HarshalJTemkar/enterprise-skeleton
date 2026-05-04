package com.enterprise.auth.demo;

import com.enterprise.common.mapper.BaseMapper;
import com.enterprise.common.mapper.EnterpriseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Example MapStruct mapper using the shared {@link EnterpriseMapperConfig}.
 *
 * <p>Inheriting {@link BaseMapper} gives you {@code toDto / toEntity} plus collection helpers
 * automatically. MapStruct generates the implementation at compile time.
 */
@Mapper(config = EnterpriseMapperConfig.class)
public interface UserMapper extends BaseMapper<UserEntity, UserDto> {

  /** Entity → DTO. The {@code passwordHash} field is never copied across. */
  @Override
  UserDto toDto(UserEntity entity);

  /**
   * DTO → Entity. {@code passwordHash} must be supplied elsewhere (e.g. a service layer) because
   * the DTO does not expose it.
   */
  @Override
  @Mapping(target = "passwordHash", ignore = true)
  UserEntity toEntity(UserDto dto);
}
