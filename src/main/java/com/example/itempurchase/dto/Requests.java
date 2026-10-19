package com.example.itempurchase.dto;

import java.util.List;

public final class Requests {

    private Requests() { }

    public record LoginRequest(String username, String password) { }

    public record RegisterRequest(String username, String password) { }

    public record ItemTypeRequest(String typeName) { }

    public record ItemRequest(String name, Long itemTypeId, String purchaseDate, Long stockAvailable, Boolean active) { }

    public record StatusRequest(Boolean active) { }

    public record LineRequest(Long itemId, Long quantity) { }

    public record PurchaseRequest(String orderId, String purchaseDate, List<LineRequest> items) { }
}
