package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProductAvailabilityServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Mock
    private NotificationService notificationService;

    private ProductAvailabilityService productAvailabilityService;

    @BeforeEach
    void setUp() {
        productAvailabilityService =
                new ProductAvailabilityService(notificationService);
    }

    @Test
    void shouldDecrementStockWhenNormalProductIsAvailable() {
        Product product = createProduct(
                ProductType.NORMAL,
                10,
                15,
                null,
                null,
                null
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(9, product.getAvailable());

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotifyDelayWhenNormalProductIsOutOfStock() {
        Product product = createProduct(
                ProductType.NORMAL,
                0,
                15,
                null,
                null,
                null
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(0, product.getAvailable());

        verify(notificationService)
                .sendDelayNotification(15, product.getName());
    }

    @Test
    void shouldDecrementStockWhenSeasonalProductIsAvailableDuringSeason() {
        Product product = createProduct(
                ProductType.SEASONAL,
                5,
                15,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(4, product.getAvailable());

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotifyDelayWhenSeasonalProductCanBeRestockedBeforeSeasonEnds() {
        Product product = createProduct(
                ProductType.SEASONAL,
                0,
                10,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        productAvailabilityService.process(product, TODAY);

        verify(notificationService)
                .sendDelayNotification(10, product.getName());
    }

    @Test
    void shouldNotifyUnavailabilityWhenRestockExceedsSeasonEnd() {
        Product product = createProduct(
                ProductType.SEASONAL,
                0,
                16,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        productAvailabilityService.process(product, TODAY);

        verify(notificationService)
                .sendOutOfStockNotification(product.getName());
    }

    @Test
    void shouldNotifyUnavailabilityWhenSeasonalProductIsOutOfSeason() {
        Product product = createProduct(
                ProductType.SEASONAL,
                5,
                10,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 14)
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(5, product.getAvailable());

        verify(notificationService)
                .sendOutOfStockNotification(product.getName());
    }

    @Test
    void shouldDecrementStockWhenExpirableProductIsNotExpired() {
        Product product = createProduct(
                ProductType.EXPIRABLE,
                5,
                10,
                TODAY.plusDays(10),
                null,
                null
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(4, product.getAvailable());

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotifyExpirationWhenProductIsExpired() {
        LocalDate expiryDate = TODAY.minusDays(1);

        Product product = createProduct(
                ProductType.EXPIRABLE,
                5,
                10,
                expiryDate,
                null,
                null
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(0, product.getAvailable());

        verify(notificationService)
                .sendExpirationNotification(product.getName(), expiryDate);
    }

    @Test
    void shouldAllowSellingProductOnItsExpiryDate() {
        Product product = createProduct(
                ProductType.EXPIRABLE,
                5,
                10,
                TODAY,
                null,
                null
        );

        productAvailabilityService.process(product, TODAY);

        assertEquals(4, product.getAvailable());

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotifyDelayWhenNonExpiredProductIsOutOfStock() {
        Product product = createProduct(
                ProductType.EXPIRABLE,
                0,
                10,
                TODAY.plusDays(10),
                null,
                null
        );

        productAvailabilityService.process(product, TODAY);

        verify(notificationService)
                .sendDelayNotification(10, product.getName());
    }

    private Product createProduct(
        ProductType type,
        int available,
        int leadTime,
        LocalDate expiryDate,
        LocalDate seasonStartDate,
        LocalDate seasonEndDate
    ) {
        Product product = new Product();

        product.setName("Test Product");
        product.setType(type);
        product.setAvailable(available);
        product.setLeadTime(leadTime);
        product.setExpiryDate(expiryDate);
        product.setSeasonStartDate(seasonStartDate);
        product.setSeasonEndDate(seasonEndDate);

        return product;
    }
}