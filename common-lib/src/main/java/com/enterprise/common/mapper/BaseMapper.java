package com.enterprise.common.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generic contract every entity↔DTO mapper should extend. It gives you
 * {@code toDto / toEntity} plus bulk list/set variants for free — MapStruct
 * generates their implementations from the single-element methods.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @Mapper(config = EnterpriseMapperConfig.class)
 * public interface UserMapper extends BaseMapper<UserEntity, UserDto> { }
 * }</pre>
 *
 * @param <E> the JPA / domain entity type
 * @param <D> the DTO / transport type
 */
public interface BaseMapper<E, D> {

    /**
     * Convert a single entity to its DTO.
     *
     * @param entity the source entity (may be {@code null})
     * @return the mapped DTO, or {@code null} when {@code entity == null}
     */
    D toDto(E entity);

    /**
     * Convert a single DTO back to an entity.
     *
     * @param dto the source DTO (may be {@code null})
     * @return the mapped entity, or {@code null} when {@code dto == null}
     */
    E toEntity(D dto);

    /**
     * Convert a collection of entities to a list of DTOs. Default
     * implementation delegates to {@link #toDto(Object)} to avoid generating
     * a second MapStruct method.
     */
    default List<D> toDtoList(Collection<E> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Convert a collection of DTOs to a list of entities.
     */
    default List<E> toEntityList(Collection<D> dtos) {
        return dtos == null ? List.of() : dtos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    /**
     * Convert a collection of entities to a {@link Set} of DTOs (useful for
     * JPA relationship mapping).
     */
    default Set<D> toDtoSet(Collection<E> entities) {
        return entities == null ? Set.of() : entities.stream().map(this::toDto).collect(Collectors.toSet());
    }
}
