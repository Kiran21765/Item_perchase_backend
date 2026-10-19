package com.example.itempurchase.service;

import com.example.itempurchase.dto.Requests.ItemRequest;
import com.example.itempurchase.dto.Responses.ItemResponse;
import com.example.itempurchase.dto.Requests.StatusRequest;
import com.example.itempurchase.entity.Item;
import com.example.itempurchase.entity.ItemType;
import com.example.itempurchase.exception.ApiException;
import com.example.itempurchase.repository.ItemRepository;
import com.example.itempurchase.repository.ItemTypeRepository;
import com.example.itempurchase.repository.PurchaseItemRepository;
import com.example.itempurchase.util.Validators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository items;
    private final ItemTypeRepository types;
    private final PurchaseItemRepository purchaseItems;

    public ItemService(ItemRepository items, ItemTypeRepository types, PurchaseItemRepository purchaseItems) {
        this.items = items;
        this.types = types;
        this.purchaseItems = purchaseItems;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> list(Boolean active, String search) {
        String needle = Validators.isBlank(search) ? null : search.trim().toLowerCase();
        return items.findAllWithType().stream()
                .filter(i -> active == null || i.isActive() == active)
                .filter(i -> needle == null || i.getName().toLowerCase().contains(needle))
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        Item item = items.findByIdWithType(id).orElseThrow(() -> ApiException.notFound("Item not found"));
        return ItemResponse.from(item)
                .withUsage(purchaseItems.countByItemId(id), purchaseItems.sumQuantityByItemId(id));
    }

    @Transactional
    public ItemResponse create(ItemRequest req) {
        Item item = new Item();
        apply(item, req);
        return ItemResponse.from(items.saveAndFlush(item));
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest req) {
        Item item = items.findById(id).orElseThrow(() -> ApiException.notFound("Item not found"));
        apply(item, req);
        return ItemResponse.from(items.saveAndFlush(item));
    }

    @Transactional
    public ItemResponse setStatus(Long id, StatusRequest req) {
        Item item = items.findById(id).orElseThrow(() -> ApiException.notFound("Item not found"));
        item.setActive(Validators.active(req == null ? null : req.active()));
        return ItemResponse.from(items.saveAndFlush(item));
    }

    /** Delete only when the item was never used in a purchase. Otherwise keep it and mark it Inactive. */
    @Transactional
    public void delete(Long id) {
        Item item = items.findById(id).orElseThrow(() -> ApiException.notFound("Item not found"));
        if (purchaseItems.existsByItemId(id)) {
            throw ApiException.conflict(
                    "Item is used in purchase history and cannot be deleted. Mark it Inactive instead");
        }
        items.delete(item);
    }

    // ---- validation shared by create and update ----
    private void apply(Item item, ItemRequest req) {
        if (req == null) throw ApiException.badRequest("Request body is required");

        String name = Validators.name(req.name(), "Item name");

        if (req.itemTypeId() == null) throw ApiException.badRequest("Item type is required");
        ItemType type = req.itemTypeId() <= 0 ? null : types.findById(req.itemTypeId()).orElse(null);
        if (type == null) throw ApiException.badRequest("Invalid item type");

        item.setName(name);
        item.setItemType(type);
        item.setPurchaseDate(Validators.date(req.purchaseDate(), "Purchase date"));
        item.setStockAvailable(Validators.stock(req.stockAvailable()));
        item.setActive(Validators.active(req.active()));
    }
}
