package com.example.itempurchase.controller;

import com.example.itempurchase.dto.Requests.ItemTypeRequest;
import com.example.itempurchase.dto.Responses.ItemTypeResponse;
import com.example.itempurchase.service.ItemTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/item-types")
public class ItemTypeController {

    private final ItemTypeService service;

    public ItemTypeController(ItemTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemTypeResponse> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<ItemTypeResponse> create(@RequestBody ItemTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ItemTypeResponse update(@PathVariable Long id, @RequestBody ItemTypeRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("message", "Item type deleted");
    }
}
