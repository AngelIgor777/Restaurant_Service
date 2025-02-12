package org.test.restaurant_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.GenericResponseService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.test.restaurant_service.dto.request.AddressRequestDTO;
import org.test.restaurant_service.dto.request.OrderProductRequestDTO;
import org.test.restaurant_service.dto.request.OrderProductRequestWithPayloadDto;
import org.test.restaurant_service.dto.request.TableRequestDTO;
import org.test.restaurant_service.dto.response.*;
import org.test.restaurant_service.entity.*;
import org.test.restaurant_service.mapper.*;
import org.test.restaurant_service.repository.ProductRepository;
import org.test.restaurant_service.service.*;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class OrderProductAndUserServiceImpl implements OrderProductAndUserService {

    private final OrderService orderService;
    private final OrderProductServiceImpl orderProductService;
    private final UserService userService;
    private final ProductDiscountService productDiscountService;
    private final DiscountService discountService;
    private final ProductRepository productRepository;
    private final OrderProductMapper orderProductMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;
    private final AddressService addressService;
    private final OrderMapper orderMapper;
    private final AddressMapper addressMapper;
    private final TableMapper tableMapper;
    private final OrderDiscountService orderDiscountService;
    private final UserAddressService userAddressService;
    private final GenericResponseService responseBuilder;

    public OrderProductAndUserServiceImpl(OrderService orderService, OrderProductServiceImpl orderProductService, UserService userService, ProductDiscountService productDiscountService, DiscountService discountService, ProductRepository productRepository, OrderProductMapper orderProductMapper, ProductMapper productMapper, @Qualifier("productServiceImpl") ProductService productService, AddressService addressService, OrderMapper orderMapper, AddressMapper addressMapper, TableMapper tableMapper, OrderDiscountService orderDiscountService, UserAddressService userAddressService, GenericResponseService responseBuilder) {
        this.orderService = orderService;
        this.orderProductService = orderProductService;
        this.userService = userService;
        this.productDiscountService = productDiscountService;
        this.discountService = discountService;
        this.productRepository = productRepository;
        this.orderProductMapper = orderProductMapper;
        this.productMapper = productMapper;
        this.productService = productService;
        this.addressService = addressService;
        this.orderMapper = orderMapper;
        this.addressMapper = addressMapper;
        this.tableMapper = tableMapper;
        this.orderDiscountService = orderDiscountService;
        this.userAddressService = userAddressService;
        this.responseBuilder = responseBuilder;
    }


    //TODO finish it
    //1 check the user is register
    //2 check the request from restaurant/outside
    //3 check the discount codes
    //build a price based on discount
    //4 save request
    //5 return all data info
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderProductResponseWithPayloadDto createBulk(OrderProductRequestWithPayloadDto requestDtoWithPayloadDto) {

        Order.PaymentMethod paymentMethod = requestDtoWithPayloadDto.getPaymentMethod();
        boolean orderInRestaurant = requestDtoWithPayloadDto.isOrderInRestaurant();
        boolean existDiscountCodes = requestDtoWithPayloadDto.isExistDiscountCodes();
        String productDiscountCode = requestDtoWithPayloadDto.getProductDiscountCode();
        String globalDiscountCode = requestDtoWithPayloadDto.getGlobalDiscountCode();

        OrderProductResponseWithPayloadDto orderProductResponseWithPayloadDto =
                OrderProductResponseWithPayloadDto.builder()
                        .orderInRestaurant(orderInRestaurant)
                        .existDiscountCodes(existDiscountCodes)
                        .build();


        Order order = Order.builder()
                .paymentMethod(paymentMethod)
                .build();


        checkTheUserIsRegistered(requestDtoWithPayloadDto, order, orderProductResponseWithPayloadDto);


        AtomicReference<BigDecimal> totalPrice = new AtomicReference<>(BigDecimal.valueOf(0));
        AtomicReference<LocalTime> totalCookingTime = new AtomicReference<>(LocalTime.of(0, 0, 0, 0));
        List<ProductResponseDTO> productResponseDTOS = new ArrayList<>();
        List<OrderProductRequestDTO> orderProductRequestDTO = requestDtoWithPayloadDto.getOrderProductRequestDTO();

        List<OrderProduct> orderProducts = getOrderProductsAndSetProductsForOrderAndCountTotalCookingTimeAndTotalPriceAndAddToProductResponseDTOList(orderProductRequestDTO, order, totalPrice, totalCookingTime, productResponseDTOS);


        BigDecimal globalDiscountAmount = BigDecimal.ZERO;
        BigDecimal productDiscountAmount = BigDecimal.ZERO;
        if (existDiscountCodes) {
            if (globalDiscountCode != null && !globalDiscountCode.isEmpty()) {
                Discount discountByCode = discountService.getDiscountByCode(globalDiscountCode);

                // Calculate the global discount percentage
                BigDecimal globalDiscountPercentage = discountByCode.getDiscount();

                // Calculate the global discount amount
                globalDiscountAmount = totalPrice.get()
                        .multiply(globalDiscountPercentage)
                        .divide(new BigDecimal(100)); // (total price * discount%) / 100

                OrderDiscount orderDiscount = OrderDiscount.builder()
                        .order(order)
                        .discount(discountByCode)
                        .build();
                orderDiscountService.save(orderDiscount);
                orderProductResponseWithPayloadDto.setGlobalDiscountCode(globalDiscountCode);
            }
            if (productDiscountCode != null && !productDiscountCode.isEmpty()) {
                // Retrieve the product and global discount details
                ProductDiscount productDiscountByCode = productDiscountService.getProductDiscountByCode(productDiscountCode);

                // Calculate the product discount percentage
                BigDecimal productDiscountPercentage = productDiscountByCode.getDiscount();

                // Apply product-specific discounts (e.g., per applicable product)
                productDiscountAmount = addToProductDiscountAmount(productResponseDTOS, productDiscountPercentage, productDiscountAmount);

                orderProductResponseWithPayloadDto.setProductDiscountCode(productDiscountCode);
            }
            BigDecimal finalTotalPrice = totalPrice.get()
                    .subtract(globalDiscountAmount)
                    .subtract(productDiscountAmount);
            totalPrice.set(finalTotalPrice);
        }
        order.setTotalPrice(totalPrice.get());

        theOrderInRestaurant(order, requestDtoWithPayloadDto, orderProductResponseWithPayloadDto);

        Order savedOrder = orderService.create(order);
        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);
        responseDTO.setTotalCookingTime(totalCookingTime.get());
        BigDecimal roundedValue = totalPrice.get().setScale(2, RoundingMode.HALF_UP);
        responseDTO.setTotalPrice(roundedValue);
        responseDTO.setProducts(productResponseDTOS);

        orderProductResponseWithPayloadDto.setOrderResponseDTO(responseDTO);

        orderProductService.createAll(orderProducts);

        orderProductService.sendOrdersFromWebsocket(orderProductResponseWithPayloadDto);

        return orderProductResponseWithPayloadDto;
    }

    private void checkTheUserIsRegistered(OrderProductRequestWithPayloadDto requestDtoWithPayloadDto, Order order, OrderProductResponseWithPayloadDto orderProductResponseWithPayloadDto) {
        if (requestDtoWithPayloadDto.isUserRegistered()) {
            UUID userUUID;
            if ((userUUID = requestDtoWithPayloadDto.getUserUUID()) != null) {
                User user = userService.findByUUID(userUUID);
                order.setUser(user);
                orderProductResponseWithPayloadDto.setUserUUID(userUUID);
            }
        }
    }

    private boolean theOrderInRestaurant(Order order, OrderProductRequestWithPayloadDto request, OrderProductResponseWithPayloadDto orderProductResponseWithPayloadDto) {
        if (request.isOrderInRestaurant()) {
            Table table = orderProductService.getByNumber(request.getTableRequestDTO().getNumber());
            order.setTable(table);
            orderProductResponseWithPayloadDto.setTableResponseDTO(tableMapper.toResponseDTO(table));
            if (order.hasUser()) {
                orderProductResponseWithPayloadDto.setUserUUID(request.getUserUUID());
            }
            return true;
        } else {
            if (order.hasUser()) {
                User user = order.getUser();
                if (user.hasAddress()) {
                    Address address = user.getAddress();
                    AddressResponseDTO responseDto = addressMapper.toResponseDto(address);
                    responseDto.setUserUUID(user.getUuid());
                    order.setAddress(address);
                    orderProductResponseWithPayloadDto.setAddressResponseDTO(responseDto);
                } else {
                    AddressRequestDTO addressRequestDTO = request.getAddressRequestDTO();
                    Address address = userAddressService.saveAddressToUser(addressRequestDTO, user.getUuid());
                    AddressResponseDTO responseDto = addressMapper.toResponseDto(address);
                    order.setAddress(address);
                    orderProductResponseWithPayloadDto.setAddressResponseDTO(responseDto);
                }
            } else {
                AddressRequestDTO addressRequestDTO = request.getAddressRequestDTO();
                Address address = addressMapper.toEntity(addressRequestDTO);
                order.setAddress(address);
                addressService.save(address);
                AddressResponseDTO responseDto = addressMapper.toResponseDto(address);
                orderProductResponseWithPayloadDto.setAddressResponseDTO(responseDto);
                return false;
            }
        }
        return false;
    }

    private BigDecimal addToProductDiscountAmount(List<ProductResponseDTO> productResponseDTOS, BigDecimal productDiscountPercentage, BigDecimal productDiscountAmount) {
        for (ProductResponseDTO productRequest : productResponseDTOS) {
            BigDecimal productPrice = productRequest.getPrice();
            BigDecimal productTotal = productPrice.multiply(new BigDecimal(productRequest.getQuantity()));

            // Calculate the discount for this product
            BigDecimal productDiscount = productTotal
                    .multiply(productDiscountPercentage)
                    .divide(new BigDecimal(100));
            productDiscountAmount = productDiscountAmount.add(productDiscount);
        }
        return productDiscountAmount;
    }


    private List<OrderProduct>
    getOrderProductsAndSetProductsForOrderAndCountTotalCookingTimeAndTotalPriceAndAddToProductResponseDTOList
            (List<OrderProductRequestDTO> requestDTOs,
             Order order,
             AtomicReference<BigDecimal> totalPrice,
             AtomicReference<LocalTime> totalCookingTime,
             List<ProductResponseDTO> productResponseDTOList) {
        return requestDTOs.stream()
                .map(requestDTO -> {
                    //todo change the finding product with productService
                    Product product = productRepository.findById(requestDTO.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("Product not found with id " + requestDTO.getProductId()));
                    OrderProduct orderProduct = createOrderProduct(order, requestDTO, product);
                    countTotalPrice(totalPrice, product, requestDTO.getQuantity());
                    countTotalCookingTime(totalCookingTime, product);
                    ProductResponseDTO productResponseDTO = productMapper.toResponseDTO(product);
                    countQuantity(requestDTO, productResponseDTO);
                    productResponseDTOList.add(productResponseDTO);
                    return orderProduct;
                })
                .collect(Collectors.toList());
    }

    private OrderProduct createOrderProduct(Order order, OrderProductRequestDTO requestDTO, Product product) {
        OrderProduct orderProduct = orderProductMapper.toEntity(requestDTO);
        orderProduct.setOrder(order);
        orderProduct.setProduct(product);
        return orderProduct;
    }


    private LocalTime countTotalCookingTime(AtomicReference<LocalTime> totalCookingTime, Product product) {
        return totalCookingTime.updateAndGet(t -> t.plusMinutes(product.getCookingTime().getMinute())
                .plusSeconds(product.getCookingTime().getSecond()));
    }

    private BigDecimal countTotalPrice(AtomicReference<BigDecimal> totalPrice, Product product, Integer quantity) {
        return totalPrice.updateAndGet(v -> v.add(product.getPrice().multiply(new BigDecimal(quantity))));
    }

    private void countQuantity(OrderProductRequestDTO requestDTO, ProductResponseDTO productResponseDTO) {
        productResponseDTO.setQuantity(requestDTO.getQuantity());
    }
}
