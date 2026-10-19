package com.example.itempurchase.util;

import com.example.itempurchase.dto.Requests.LineRequest;
import com.example.itempurchase.exception.ApiException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** All backend validation rules in one place (messages follow the assignment's error table). */
public final class Validators {

    private static final long MAX_STOCK = 1_000_000_000L;
    private static final long MAX_QUANTITY = 1_000_000L;
    private static final Pattern ORDER_ID = Pattern.compile("^[A-Z0-9_-]{1,30}$");

    private Validators() { }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String name(String value, String label) {
        if (isBlank(value)) throw ApiException.badRequest(label + " is required");
        String text = value.trim();
        if (text.length() > 100) throw ApiException.badRequest(label + " must be 100 characters or less");
        return text;
    }

    public static LocalDate date(String value, String label) {
        if (isBlank(value)) throw ApiException.badRequest(label + " is required");
        try {
            return LocalDate.parse(value.trim());          // strict ISO yyyy-MM-dd, rejects 2026-02-30
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(label + " is invalid. Use a real date in format YYYY-MM-DD");
        }
    }

    public static int stock(Long value) {
        if (value == null) throw ApiException.badRequest("Stock is required");
        if (value < 0) throw ApiException.badRequest("Stock cannot be negative");
        if (value > MAX_STOCK) throw ApiException.badRequest("Stock value is too large");
        return value.intValue();
    }

    public static int quantity(Long value) {
        if (value == null) throw ApiException.badRequest("Quantity is required");
        if (value <= 0) throw ApiException.badRequest("Quantity must be greater than zero");
        if (value > MAX_QUANTITY) throw ApiException.badRequest("Quantity is too large");
        return value.intValue();
    }

    public static boolean active(Boolean value) {
        if (value == null) throw ApiException.badRequest("Status (active/inactive) is required");
        return value;
    }

    public static String orderId(String value) {
        String text = value.trim().toUpperCase();
        if (!ORDER_ID.matcher(text).matches()) {
            throw ApiException.badRequest(
                    "Order ID may only contain letters, numbers, '-' and '_' (max 30 characters)");
        }
        return text;
    }

    /**
     * Validates purchase lines and returns itemId -> quantity (keeps the order).
     * Duplicate items in one order are rejected (one consistent rule for create and update).
     */
    public static Map<Long, Integer> lines(List<LineRequest> items) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("Purchase must contain at least one item");
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        Set<Long> seen = new HashSet<>();
        int position = 0;
        for (LineRequest line : items) {
            position++;
            if (line == null) throw ApiException.badRequest("Invalid purchase line at position " + position);
            if (line.itemId() == null) throw ApiException.badRequest("Item is required for every purchase line");
            if (line.itemId() <= 0) throw ApiException.badRequest("Invalid item ID");
            if (!seen.add(line.itemId())) {
                throw ApiException.badRequest("Duplicate item in the same order (item ID " + line.itemId()
                        + "). Combine the quantities into one line");
            }
            result.put(line.itemId(), quantity(line.quantity()));
        }
        return result;
    }

    /** Required: >0 In Stock, =0 Out of Stock. Optional: <=5 Low Stock. */
    public static String availability(int stock) {
        if (stock <= 0) return "Out of Stock";
        if (stock <= 5) return "Low Stock";
        return "In Stock";
    }
}
