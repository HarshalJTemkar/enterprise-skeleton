package com.enterprise.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct configuration used by every mapper in the platform.
 *
 * <p>Apply via {@code @Mapper(config = EnterpriseMapperConfig.class)}. The defaults below are
 * intentionally permissive to reduce boilerplate:
 *
 * <ul>
 *   <li>{@code componentModel=spring} – mappers are Spring beans.
 *   <li>{@code unmappedTargetPolicy=IGNORE} – silent on unmapped fields.
 *   <li>{@code nullValuePropertyMappingStrategy=IGNORE} – keep target value when the source is
 *       {@code null} (safer for partial updates).
 *   <li>{@code nullValueCheckStrategy=ALWAYS} – defensive null checks.
 * </ul>
 */
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface EnterpriseMapperConfig {}
