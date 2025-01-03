package org.test.restaurant_service.service;

import org.test.restaurant_service.dto.request.OrderRequestDTO;
import org.test.restaurant_service.dto.response.OrderResponseDTO;

import java.util.List;

public interface OrderService {
    OrderResponseDTO create(OrderRequestDTO requestDTO);
    List<OrderResponseDTO> getAllOrders();
    OrderResponseDTO getOrderById(Integer id);
    OrderResponseDTO update(Integer id, OrderRequestDTO requestDTO);
    void delete(Integer id);
}
