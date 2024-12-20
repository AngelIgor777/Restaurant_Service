package org.test.restaurant_service.service;

import org.springframework.data.domain.*;
import org.test.restaurant_service.dto.request.TableRequestDTO;
import org.test.restaurant_service.dto.response.TableResponseDTO;

public interface TableService {
    Page<TableResponseDTO> getAll(Pageable pageable);

    TableResponseDTO getById(Integer id);

    TableResponseDTO create(TableRequestDTO tableRequestDTO);

    TableResponseDTO update(Integer id, TableRequestDTO tableRequestDTO);

    void deleteById(Integer id);
}
