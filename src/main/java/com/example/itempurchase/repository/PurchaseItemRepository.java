package com.example.itempurchase.repository;

import com.example.itempurchase.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    boolean existsByItemId(Long itemId);

    long countByItemId(Long itemId);

    @Query("select coalesce(sum(pi.quantity), 0L) from PurchaseItem pi where pi.item.id = :itemId")
    Long sumQuantityByItemId(@Param("itemId") Long itemId);
}
