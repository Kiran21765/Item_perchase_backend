package com.example.itempurchase.controller;

import com.example.itempurchase.dto.Requests.PurchaseRequest;
import com.example.itempurchase.dto.Responses.PurchaseResponse;
import com.example.itempurchase.dto.Responses.PurchaseSummary;
import com.example.itempurchase.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Purchases: create, list, view, update.
 * There is NO @DeleteMapping in this class on purpose: DELETE /api/purchases/{id} answers 405.
 */
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @GetMapping
    public List<PurchaseSummary> list() {
        return service.list();
    }

    @GetMapping("/next-order-id")
    public Map<String, String> nextOrderId() {
        return Map.of("orderId", service.nextOrderId());
    }

    // {ref} = numeric id (12) or order id (PO-00012)
    @GetMapping("/{ref}")
    public PurchaseResponse get(@PathVariable String ref) {
        return service.get(ref);
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@RequestBody PurchaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{ref}")
    public PurchaseResponse update(@PathVariable String ref, @RequestBody PurchaseRequest req) {
        return service.update(ref, req);
    }
}
