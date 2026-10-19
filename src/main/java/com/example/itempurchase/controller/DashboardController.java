package com.example.itempurchase.controller;

import com.example.itempurchase.dto.Responses.DashboardResponse;
import com.example.itempurchase.dto.Responses.PurchaseSummary;
import com.example.itempurchase.entity.Item;
import com.example.itempurchase.repository.ItemRepository;
import com.example.itempurchase.repository.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Read-only summary for the dashboard page (GET only, any logged-in user). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ItemRepository items;
    private final PurchaseRepository purchases;

    public DashboardController(ItemRepository items, PurchaseRepository purchases) {
        this.items = items;
        this.purchases = purchases;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardResponse summary(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();

        long in = 0, low = 0, out = 0;
        List<Item> all = items.findAll();
        for (Item i : all) {
            int s = i.getStockAvailable();
            if (s <= 0) out++;
            else if (s <= 5) low++;
            else in++;
        }

        List<Long> monthly = new ArrayList<>(java.util.Collections.nCopies(12, 0L));
        List<PurchaseSummary> list = purchases.findSummaries();
        long units = 0;
        for (PurchaseSummary p : list) {
            units += p.totalQuantity();
            if (p.purchaseDate().getYear() == y) {
                int m = p.purchaseDate().getMonthValue() - 1;
                monthly.set(m, monthly.get(m) + p.totalQuantity());
            }
        }
        return new DashboardResponse(all.size(), in, low, out, list.size(), units, y, monthly);
    }
}
