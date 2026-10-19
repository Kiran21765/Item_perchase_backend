package com.example.itempurchase.controller;

import com.example.itempurchase.dto.Requests.ItemRequest;
import com.example.itempurchase.dto.Responses.ItemResponse;
import com.example.itempurchase.dto.Requests.StatusRequest;
import com.example.itempurchase.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    // GET /api/items?active=true&search=lap
    @GetMapping
    public List<ItemResponse> list(@RequestParam(required = false) Boolean active,
                                   @RequestParam(required = false) String search) {
        return service.list(active, search);
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@RequestBody ItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @RequestBody ItemRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public ItemResponse setStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        return service.setStatus(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("message", "Item deleted");
    }
}
