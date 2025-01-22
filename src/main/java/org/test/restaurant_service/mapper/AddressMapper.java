package org.test.restaurant_service.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.test.restaurant_service.dto.request.AddressRequestDTO;
import org.test.restaurant_service.dto.response.AddressResponseDTO;
import org.test.restaurant_service.entity.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "id",ignore = true)
    @Mapping(target = "isRegisterUser",ignore = true)
    AddressResponseDTO toAddressResponseDTO(AddressRequestDTO addressRequestDTO);

    @Mapping(target = "isRegisterUser",ignore = true)
    @Mapping(target = "userId",ignore = true)
    AddressResponseDTO toResponseDto(Address address);
}