package com.example.itempurchase.service;

import com.example.itempurchase.dto.Requests.ItemTypeRequest;
import com.example.itempurchase.dto.Responses.ItemTypeResponse;
import com.example.itempurchase.entity.ItemType;
import com.example.itempurchase.exception.ApiException;
import com.example.itempurchase.repository.ItemRepository;
import com.example.itempurchase.repository.ItemTypeRepository;
import com.example.itempurchase.util.Validators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemTypeService {

    private final ItemTypeRepository types;
    private final ItemRepository items;

    public ItemTypeService(ItemTypeRepository types, ItemRepository items) {
        this.types = types;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<ItemTypeResponse> list() {
        return types.findAllWithCount();
    }

    @Transactional
    public ItemTypeResponse create(ItemTypeRequest req) {
        String name = Validators.name(req == null ? null : req.typeName(), "Item type name");
        if (types.findByTypeNameIgnoreCase(name).isPresent()) {
            throw ApiException.conflict("Item type already exists");
        }
        ItemType type = new ItemType();
        type.setTypeName(name);
        type = types.save(type);
        return new ItemTypeResponse(type.getId(), type.getTypeName(), 0L);
    }

    @Transactional
    public ItemTypeResponse update(Long id, ItemTypeRequest req) {
        ItemType type = types.findById(id).orElseThrow(() -> ApiException.notFound("Item type not found"));
        String name = Validators.name(req == null ? null : req.typeName(), "Item type name");
        types.findByTypeNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) throw ApiException.conflict("Item type already exists");
        });
        type.setTypeName(name);
        types.save(type);
        return new ItemTypeResponse(type.getId(), type.getTypeName(), items.countByItemTypeId(id));
    }

    @Transactional
    public void delete(Long id) {
        ItemType type = types.findById(id).orElseThrow(() -> ApiException.notFound("Item type not found"));
        if (items.countByItemTypeId(id) > 0) {
            throw ApiException.conflict("Item type is used by existing items and cannot be deleted");
        }
        types.delete(type);
    }
}
