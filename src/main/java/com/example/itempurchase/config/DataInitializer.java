package com.example.itempurchase.config;

import com.example.itempurchase.entity.AppUser;
import com.example.itempurchase.entity.Item;
import com.example.itempurchase.entity.ItemType;
import com.example.itempurchase.entity.Role;
import com.example.itempurchase.dto.Requests.LineRequest;
import com.example.itempurchase.dto.Requests.PurchaseRequest;
import com.example.itempurchase.repository.AppUserRepository;
import com.example.itempurchase.repository.ItemRepository;
import com.example.itempurchase.repository.ItemTypeRepository;
import com.example.itempurchase.service.PurchaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Runs once at startup: creates the two demo accounts and (optionally) sample item types and items. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository users;
    private final ItemTypeRepository types;
    private final ItemRepository items;
    private final PasswordEncoder encoder;
    private final PurchaseService purchaseService;
    private final boolean seedData;

    public DataInitializer(AppUserRepository users, ItemTypeRepository types, ItemRepository items,
                           PasswordEncoder encoder, PurchaseService purchaseService, @Value("${app.seed-data:true}") boolean seedData) {
        this.users = users;
        this.types = types;
        this.items = items;
        this.encoder = encoder;
        this.purchaseService = purchaseService;
        this.seedData = seedData;
    }

    @Override
    public void run(String... args) {
        ensureUser("admin", "admin123", Role.ADMIN);
        ensureUser("user", "user123", Role.USER);

        if (seedData && types.count() == 0) {
            ItemType electronics = type("Electronics");
            ItemType furniture = type("Furniture");
            ItemType clothing = type("Clothing");
            ItemType grocery = type("Grocery");
            ItemType stationery = type("Stationery");

            item("Laptop", "2026-08-01", 10, electronics, true);
            item("Mouse", "2026-08-01", 20, electronics, true);
            item("Keyboard", "2026-08-02", 12, electronics, true);
            item("Chair", "2026-08-03", 15, furniture, true);
            item("T-Shirt", "2026-08-04", 30, clothing, true);
            item("Rice 5kg", "2026-08-05", 4, grocery, true);
            item("Notebook", "2026-08-06", 0, stationery, true);
            item("Old Monitor", "2026-07-15", 6, electronics, false);

            seedPurchases();
        }
    }

    /** A few past orders (through the real service, so stock is deducted) so the dashboard chart has data. */
    private void seedPurchases() {
        List<Item> all = items.findAll();
        java.util.function.Function<String, Long> id = n ->
                all.stream().filter(i -> i.getName().equals(n)).findFirst().orElseThrow().getId();
        order(7, new LineRequest(id.apply("Laptop"), 1L), new LineRequest(id.apply("Mouse"), 2L));
        order(6, new LineRequest(id.apply("Chair"), 2L));
        order(5, new LineRequest(id.apply("Mouse"), 3L), new LineRequest(id.apply("Keyboard"), 1L));
        order(4, new LineRequest(id.apply("T-Shirt"), 4L));
        order(3, new LineRequest(id.apply("Laptop"), 2L), new LineRequest(id.apply("Chair"), 1L));
        order(2, new LineRequest(id.apply("Mouse"), 4L), new LineRequest(id.apply("Keyboard"), 2L));
        order(1, new LineRequest(id.apply("T-Shirt"), 5L), new LineRequest(id.apply("Chair"), 3L),
                new LineRequest(id.apply("Keyboard"), 5L));
        order(0, new LineRequest(id.apply("Laptop"), 1L), new LineRequest(id.apply("Mouse"), 2L));
    }

    private void order(int monthsAgo, LineRequest... lines) {
        String date = LocalDate.now().minusMonths(monthsAgo).toString();
        purchaseService.create(new PurchaseRequest(null, date, List.of(lines)));
    }

    private void ensureUser(String username, String rawPassword, Role role) {
        if (!users.existsByUsernameIgnoreCase(username)) {
            users.save(new AppUser(username, encoder.encode(rawPassword), role));
        }
    }

    private ItemType type(String name) {
        ItemType t = new ItemType();
        t.setTypeName(name);
        return types.save(t);
    }

    private void item(String name, String date, int stock, ItemType type, boolean active) {
        Item i = new Item();
        i.setName(name);
        i.setPurchaseDate(LocalDate.parse(date));
        i.setStockAvailable(stock);
        i.setItemType(type);
        i.setActive(active);
        items.save(i);
    }
}
