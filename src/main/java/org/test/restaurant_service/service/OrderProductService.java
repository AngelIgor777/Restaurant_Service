package org.test.restaurant_service.service;

import org.test.restaurant_service.dto.request.OrderProductRequestDTO;
import org.test.restaurant_service.dto.response.OrderProductResponseDTO;
import org.test.restaurant_service.entity.Order;

import java.util.List;

public interface OrderProductService {

    List<OrderProductResponseDTO> createBulk(List<OrderProductRequestDTO> requestDTOs, Integer tableNumber, Order.PaymentMethod paymentMethod);

    List<OrderProductResponseDTO> getOrderProductsByOrderId(Integer orderId);

    OrderProductResponseDTO update(Integer id, OrderProductRequestDTO requestDTO, Integer orderId);

    void delete(Integer id);
}
