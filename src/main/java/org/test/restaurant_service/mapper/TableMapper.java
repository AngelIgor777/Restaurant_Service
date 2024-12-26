package org.test.restaurant_service.mapper;

import org.mapstruct.*;
import org.test.restaurant_service.dto.request.TableRequestDTO;
import org.test.restaurant_service.dto.response.TableResponseDTO;
import org.test.restaurant_service.entity.Table;

@Mapper(componentModel = "spring")
public interface TableMapper {

    @Mapping(target = "id", ignore = true) // Если id не нужно маппировать
    Table toEntity(TableRequestDTO tableRequestDTO);

    TableResponseDTO toResponseDTO(Table table);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequestDTO(TableRequestDTO requestDTO, @MappingTarget Table table);
}
