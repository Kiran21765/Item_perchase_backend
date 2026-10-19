package com.example.itempurchase.dto;

import com.example.itempurchase.entity.Item;
import com.example.itempurchase.util.Validators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Responses {

    private Responses() { }

    public record AuthResponse(String token, String username, String role) { }

    /** Numbers for the dashboard cards and charts. monthlyUnits has 12 values (Jan..Dec) for the given year. */
    public record DashboardResponse(long totalItems, long inStock, long lowStock, long outOfStock,
                                    long totalOrders, long totalUnitsPurchased,
                                    int year, List<Long> monthlyUnits) { }

    /** purchaseCount / totalPurchased / canDelete are only filled for the "view details" call. */
    public record ItemResponse(Long id, String name, Long itemTypeId, String typeName, LocalDate purchaseDate,
                               Integer stockAvailable, boolean active, String availability,
                               LocalDateTime createdAt, LocalDateTime updatedAt,
                               Long purchaseCount, Long totalPurchased, Boolean canDelete) {

        public static ItemResponse from(Item i) {
            return new ItemResponse(i.getId(), i.getName(), i.getItemType().getId(), i.getItemType().getTypeName(),
                    i.getPurchaseDate(), i.getStockAvailable(), i.isActive(),
                    Validators.availability(i.getStockAvailable()), i.getCreatedAt(), i.getUpdatedAt(),
                    null, null, null);
        }

        public ItemResponse withUsage(long purchaseCount, long totalPurchased) {
            return new ItemResponse(id, name, itemTypeId, typeName, purchaseDate, stockAvailable, active, availability,
                    createdAt, updatedAt, purchaseCount, totalPurchased, purchaseCount == 0);
        }
    }

    public record ItemTypeResponse(Long id, String typeName, Long itemCount) { }

    public record PurchaseLineResponse(Long itemId, String itemName, String typeName, Integer quantity,
                                       Integer currentStock, String availability, boolean active) { }

    public record PurchaseResponse(Long id, String orderId, LocalDate purchaseDate, LocalDateTime createdAt,
                                   LocalDateTime updatedAt, int totalQuantity, List<PurchaseLineResponse> items) { }

    public record PurchaseSummary(Long id, String orderId, LocalDate purchaseDate, Long itemLines, Long totalQuantity) { }
}
