package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductAvailabilityService productAvailabilityService;

    private OrderProcessingService orderProcessingService;

    private static final Long ORDER_ID = 1L;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T12:00:00Z"),
            ZoneOffset.UTC
    );

    @BeforeEach
    void setUp() {
        orderProcessingService = new OrderProcessingService(
                orderRepository,
                productAvailabilityService,
                FIXED_CLOCK
        );
    }

    @Test
    void shouldProcessAllProductsWhenOrderExists() {
        Product firstProduct = new Product();
        firstProduct.setType(ProductType.NORMAL);

        Product secondProduct = new Product();
        secondProduct.setType(ProductType.EXPIRABLE);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setItems(Set.of(firstProduct, secondProduct));

        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.of(order));

        ProcessOrderResponse response =
                orderProcessingService.processOrder(ORDER_ID);

        assertEquals(ORDER_ID, response.id());

        verify(orderRepository).findById(ORDER_ID);

        verify(productAvailabilityService, times(2))
                .process(any(Product.class), eq(
                        java.time.LocalDate.of(2026, 6, 15)
                ));
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findById(ORDER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderProcessingService.processOrder(ORDER_ID)
        );

        verifyNoInteractions(productAvailabilityService);
    }
}