package com.example.itempurchase.service;

import com.example.itempurchase.dto.Responses.PurchaseLineResponse;
import com.example.itempurchase.dto.Requests.PurchaseRequest;
import com.example.itempurchase.dto.Responses.PurchaseResponse;
import com.example.itempurchase.dto.Responses.PurchaseSummary;
import com.example.itempurchase.entity.Item;
import com.example.itempurchase.entity.Purchase;
import com.example.itempurchase.entity.PurchaseItem;
import com.example.itempurchase.exception.ApiException;
import com.example.itempurchase.repository.ItemRepository;
import com.example.itempurchase.repository.PurchaseRepository;
import com.example.itempurchase.util.Validators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Purchase business logic. Every write method is @Transactional:
 * if ANY exception is thrown, Spring rolls the whole transaction back,
 * so no partial purchase is saved and stock is never changed.
 */
@Service
public class PurchaseService {

    private final PurchaseRepository purchases;
    private final ItemRepository items;

    public PurchaseService(PurchaseRepository purchases, ItemRepository items) {
        this.purchases = purchases;
        this.items = items;
    }

    // ------------------------------------------------------------------ reads
    @Transactional(readOnly = true)
    public List<PurchaseSummary> list() {
        return purchases.findSummaries();
    }

    @Transactional(readOnly = true)
    public String nextOrderId() {
        return generateOrderId();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(String ref) {
        Purchase p = (isNumeric(ref)
                ? purchases.findDetailById(Long.parseLong(ref))
                : purchases.findDetailByOrderId(Validators.orderId(ref)))
                .orElseThrow(() -> ApiException.notFound("Purchase not found"));
        return toResponse(p);
    }

    // ----------------------------------------------------------------- create
    @Transactional
    public PurchaseResponse create(PurchaseRequest req) {
        if (req == null) throw ApiException.badRequest("Request body is required");
        LocalDate date = Validators.date(req.purchaseDate(), "Purchase date");
        Map<Long, Integer> lines = Validators.lines(req.items());
        String requestedOrderId = Validators.isBlank(req.orderId()) ? null : Validators.orderId(req.orderId());

        // 1) lock + validate every item (exists, active, enough stock)
        Map<Long, Item> locked = lockItems(lines.keySet());
        for (Map.Entry<Long, Integer> line : lines.entrySet()) {
            Item item = locked.get(line.getKey());
            if (item == null) throw ApiException.notFound("Item not found (ID " + line.getKey() + ")");
            if (!item.isActive()) {
                throw ApiException.conflict("Item is inactive and cannot be purchased: " + item.getName());
            }
            if (line.getValue() > item.getStockAvailable()) {
                throw insufficient(item, line.getValue(), "requested");
            }
        }

        // 2) unique order id
        String orderId = requestedOrderId != null ? requestedOrderId : generateOrderId();
        if (requestedOrderId != null && purchases.existsByOrderId(orderId)) {
            throw ApiException.conflict("Order ID " + orderId + " already exists");
        }

        // 3) insert purchase + purchase items, deduct stock
        Purchase purchase = new Purchase();
        purchase.setOrderId(orderId);
        purchase.setPurchaseDate(date);
        for (Map.Entry<Long, Integer> line : lines.entrySet()) {
            Item item = locked.get(line.getKey());
            item.setStockAvailable(item.getStockAvailable() - line.getValue());
            purchase.getItems().add(new PurchaseItem(purchase, item, line.getValue()));
        }
        purchases.save(purchase);          // COMMIT happens when this method returns normally
        return toResponse(purchase);
    }

    // ----------------------------------------------------------------- update
    /**
     * body = the NEW full list of lines. For every item: difference = newQty - oldQty
     *  difference > 0 : item must be active and have that much stock -> stock is reduced
     *  difference < 0 : stock is returned
     *  line removed   : its old quantity is returned to stock
     */
    @Transactional
    public PurchaseResponse update(String ref, PurchaseRequest req) {
        if (req == null) throw ApiException.badRequest("Request body is required");
        LocalDate date = Validators.date(req.purchaseDate(), "Purchase date");
        Map<Long, Integer> newLines = Validators.lines(req.items());

        Purchase purchase = (isNumeric(ref)
                ? purchases.lockById(Long.parseLong(ref))
                : purchases.lockByOrderId(Validators.orderId(ref)))
                .orElseThrow(() -> ApiException.notFound("Purchase not found"));

        if (!Validators.isBlank(req.orderId())
                && !Validators.orderId(req.orderId()).equals(purchase.getOrderId())) {
            throw ApiException.badRequest("Order ID cannot be changed");
        }

        Map<Long, PurchaseItem> oldLines = new LinkedHashMap<>();
        for (PurchaseItem pi : purchase.getItems()) {
            oldLines.put(pi.getItem().getId(), pi);
        }

        Set<Long> allIds = new TreeSet<>(oldLines.keySet());
        allIds.addAll(newLines.keySet());
        Map<Long, Item> locked = lockItems(allIds);

        // validate first ...
        for (Long id : allIds) {
            Item item = locked.get(id);
            if (item == null) throw ApiException.notFound("Item not found (ID " + id + ")");
            int diff = newLines.getOrDefault(id, 0) - quantityOf(oldLines.get(id));
            if (diff > 0) {
                if (!item.isActive()) {
                    throw ApiException.conflict("Item is inactive and cannot be purchased: " + item.getName());
                }
                if (diff > item.getStockAvailable()) {
                    throw insufficient(item, diff, "additional quantity needed");
                }
            }
        }

        // ... then change
        for (Long id : allIds) {
            Item item = locked.get(id);
            PurchaseItem line = oldLines.get(id);
            int before = quantityOf(line);
            int after = newLines.getOrDefault(id, 0);
            int diff = after - before;

            if (after == 0) {
                purchase.getItems().remove(line);                       // orphanRemoval deletes the row
            } else if (line == null) {
                purchase.getItems().add(new PurchaseItem(purchase, item, after));
            } else if (diff != 0) {
                line.setQuantity(after);
            }
            if (diff != 0) {
                item.setStockAvailable(item.getStockAvailable() - diff);   // diff < 0 adds stock back
            }
        }

        purchase.setPurchaseDate(date);
        purchase.touch();
        purchases.save(purchase);
        return toResponse(purchase);
    }

    // ---------------------------------------------------------------- helpers
    private Map<Long, Item> lockItems(Set<Long> ids) {
        Map<Long, Item> map = new HashMap<>();
        for (Item item : items.findAllForUpdate(ids)) {
            map.put(item.getId(), item);
        }
        return map;
    }

    private int quantityOf(PurchaseItem line) {
        return line == null ? 0 : line.getQuantity();
    }

    private boolean isNumeric(String ref) {
        return ref != null && ref.matches("\\d{1,18}");
    }

    private String generateOrderId() {
        long n = purchases.count() + 1;
        String id;
        do {
            id = String.format("PO-%05d", n++);
        } while (purchases.existsByOrderId(id));
        return id;
    }

    private ApiException insufficient(Item item, int requested, String label) {
        return ApiException.conflict(
                "Insufficient stock for \"" + item.getName() + "\". Available: " + item.getStockAvailable()
                        + ", " + label + ": " + requested,
                Map.of("itemId", item.getId(), "itemName", item.getName(),
                        "available", item.getStockAvailable(), "requested", requested));
    }

    private PurchaseResponse toResponse(Purchase p) {
        List<PurchaseLineResponse> lines = p.getItems().stream().map(pi -> {
            Item i = pi.getItem();
            return new PurchaseLineResponse(i.getId(), i.getName(), i.getItemType().getTypeName(), pi.getQuantity(),
                    i.getStockAvailable(), Validators.availability(i.getStockAvailable()), i.isActive());
        }).toList();
        int total = lines.stream().mapToInt(PurchaseLineResponse::quantity).sum();
        return new PurchaseResponse(p.getId(), p.getOrderId(), p.getPurchaseDate(), p.getCreatedAt(),
                p.getUpdatedAt(), total, lines);
    }
}
