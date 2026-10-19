package com.example.itempurchase.repository;

import com.example.itempurchase.entity.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // JOIN #1 required by the assignment: Item + Item Type
    @Query("select i from Item i join fetch i.itemType it order by i.id")
    List<Item> findAllWithType();

    @Query("select i from Item i join fetch i.itemType it where i.id = :id")
    Optional<Item> findByIdWithType(@Param("id") Long id);

    // SELECT ... FOR UPDATE : stops two purchases from using the same stock at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id in :ids order by i.id")
    List<Item> findAllForUpdate(@Param("ids") Collection<Long> ids);

    long countByItemTypeId(Long itemTypeId);
}
