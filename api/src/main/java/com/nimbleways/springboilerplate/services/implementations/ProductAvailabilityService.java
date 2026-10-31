package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductAvailabilityService {

    private final NotificationService notificationService;

    public void process(Product product, LocalDate today) {
        Objects.requireNonNull(product, "Product must not be null");
        Objects.requireNonNull(today, "Processing date must not be null");

        ProductType type = Objects.requireNonNull(
                product.getType(),
                "Product type must not be null"
        );

        switch (type) {
            case NORMAL -> processNormalProduct(product);

            case SEASONAL -> processSeasonalProduct(product, today);

            case EXPIRABLE -> processExpirableProduct(product, today);
        }
    }

    private void processNormalProduct(Product product) {
        int available = getAvailableStock(product);

        if (available > 0) {
            decrementStock(product, available);
            return;
        }

        notifyDelay(product);
    }

    private void processSeasonalProduct(Product product, LocalDate today) {
        LocalDate seasonStartDate = requireDate(
                product.getSeasonStartDate(),
                "Season start date"
        );

        LocalDate seasonEndDate = requireDate(
                product.getSeasonEndDate(),
                "Season end date"
        );

        if (seasonStartDate.isAfter(seasonEndDate)) {
            throw new IllegalStateException(
                    "Season start date must not be after season end date"
            );
        }

        if (isOutsideSeason(today, seasonStartDate, seasonEndDate)) {
            notifyUnavailability(product);
            return;
        }

        int available = getAvailableStock(product);

        if (available > 0) {
            decrementStock(product, available);
            return;
        }

        int leadTime = getLeadTime(product);

        LocalDate expectedRestockDate = today.plusDays(leadTime);

        if (expectedRestockDate.isAfter(seasonEndDate)) {
            notifyUnavailability(product);
            return;
        }

        notifyDelay(product);
    }

    private void processExpirableProduct(Product product, LocalDate today) {
        LocalDate expiryDate = requireDate(
                product.getExpiryDate(),
                "Product expiry date"
        );

        int available = getAvailableStock(product);

        if (today.isAfter(expiryDate)) {
            product.setAvailable(0);

            notificationService.sendExpirationNotification(
                    product.getName(),
                    expiryDate
            );

            return;
        }

        if (available > 0) {
            decrementStock(product, available);
            return;
        }

        notifyDelay(product);
    }

    private boolean isOutsideSeason(
            LocalDate today,
            LocalDate seasonStartDate,
            LocalDate seasonEndDate
    ) {
        return today.isBefore(seasonStartDate)
                || today.isAfter(seasonEndDate);
    }

    private void decrementStock(Product product, int available) {
        product.setAvailable(available - 1);
    }

    private int getAvailableStock(Product product) {
        Integer available = product.getAvailable();

        if (available == null || available < 0) {
            throw new IllegalStateException(
                    "Product available stock must be a non-negative number"
            );
        }

        return available;
    }

    private int getLeadTime(Product product) {
        Integer leadTime = product.getLeadTime();

        if (leadTime == null || leadTime < 0) {
            throw new IllegalStateException(
                    "Product lead time must be a non-negative number"
            );
        }

        return leadTime;
    }

    private LocalDate requireDate(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalStateException(fieldName + " must not be null");
        }

        return date;
    }

    private void notifyDelay(Product product) {
        notificationService.sendDelayNotification(
                getLeadTime(product),
                product.getName()
        );
    }

    private void notifyUnavailability(Product product) {
        notificationService.sendOutOfStockNotification(
                product.getName()
        );
    }
}