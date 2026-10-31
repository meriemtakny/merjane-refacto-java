
package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldPersistStockDecrementAfterProcessingOrder() throws Exception {
        // Arrange
        Product product = createProduct(
                "USB Cable",
                ProductType.NORMAL,
                10,
                15
        );

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        Long productId = product.getId();
        Long orderId = order.getId();

        // Act
        mockMvc.perform(
                post("/orders/{orderId}/processOrder", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        // Assert
        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow();

        assertEquals(
                Integer.valueOf(9),
                updatedProduct.getAvailable()
        );

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotifyDelayWhenProductIsOutOfStock() throws Exception {
        // Arrange
        Product product = createProduct(
                "USB Cable",
                ProductType.NORMAL,
                0,
                15
        );

        product = productRepository.save(product);

        Order order = createOrder(Set.of(product));
        order = orderRepository.save(order);

        Long productId = product.getId();

        // Act
        mockMvc.perform(
                post("/orders/{orderId}/processOrder", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());

        // Assert
        Product unchangedProduct = productRepository.findById(productId)
                .orElseThrow();

        assertEquals(
                Integer.valueOf(0),
                unchangedProduct.getAvailable()
        );

        verify(notificationService).sendDelayNotification(
                15,
                "USB Cable"
        );
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Arrange
        Long unknownOrderId = 999L;

        // Act & Assert
        mockMvc.perform(
                post("/orders/{orderId}/processOrder", unknownOrderId)
        )
                .andExpect(status().isNotFound());

        verifyNoInteractions(notificationService);
    }

    private Product createProduct(
            String name,
            ProductType type,
            Integer available,
            Integer leadTime
    ) {
        Product product = new Product();

        product.setName(name);
        product.setType(type);
        product.setAvailable(available);
        product.setLeadTime(leadTime);

        return product;
    }

    private Order createOrder(Set<Product> products) {
        Order order = new Order();
        order.setItems(products);

        return order;
    }
}
