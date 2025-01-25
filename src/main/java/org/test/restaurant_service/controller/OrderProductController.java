package org.test.restaurant_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.test.restaurant_service.dto.request.OrderProductRequestDTO;
import org.test.restaurant_service.dto.request.OrderProductRequestWithPayloadDto;
import org.test.restaurant_service.dto.response.OrderProductResponseDTO;
import org.test.restaurant_service.dto.response.OrderProductResponseWithPayloadDto;
import org.test.restaurant_service.entity.OrderProduct;
import org.test.restaurant_service.mapper.OrderProductMapper;
import org.test.restaurant_service.service.OrderProductAndUserService;
import org.test.restaurant_service.service.OrderProductService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/order-products")
@RequiredArgsConstructor
public class OrderProductController {

    private final OrderProductService orderProductService;
    private final OrderProductAndUserService orderProductAndUserService;
    private final OrderProductMapper orderProductMapper;

    @GetMapping("/order/{orderId}")
    public List<OrderProductResponseDTO> getOrderProductsByOrderId(@PathVariable Integer orderId) {
        List<OrderProduct> orderProductsByOrderId = orderProductService.getOrderProductsByOrderId(orderId);
        return orderProductsByOrderId.stream().map(orderProductMapper::toResponseDTO).toList();
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderProductResponseWithPayloadDto createBulk(@Valid @RequestBody OrderProductRequestWithPayloadDto requestDtoWithPayloadDto) {
        return orderProductAndUserService.createBulk(requestDtoWithPayloadDto);
    }

    @PatchMapping("/{id}")
    public OrderProductResponseDTO update(@PathVariable Integer id,
                                          @Valid @RequestBody OrderProductRequestDTO requestDTO,
                                          @RequestParam Integer orderId) {
        return orderProductService.update(id, requestDTO, orderId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        orderProductService.delete(id);
    }
}
