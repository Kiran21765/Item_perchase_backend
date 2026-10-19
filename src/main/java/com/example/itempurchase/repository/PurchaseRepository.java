package com.example.itempurchase.repository;

import com.example.itempurchase.dto.Responses.PurchaseSummary;
import com.example.itempurchase.entity.Purchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Note: delete methods are deliberately never used. Purchases are historical records. */
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByOrderId(String orderId);

    // JOIN #2 required by the assignment: Purchase + Purchase Items + Items + Item Types
    @Query("select p from Purchase p join fetch p.items pi join fetch pi.item i join fetch i.itemType it "
            + "where p.orderId = :orderId")
    Optional<Purchase> findDetailByOrderId(@Param("orderId") String orderId);

    @Query("select p from Purchase p join fetch p.items pi join fetch pi.item i join fetch i.itemType it "
            + "where p.id = :id")
    Optional<Purchase> findDetailById(@Param("id") Long id);

    @Query("select new com.example.itempurchase.dto.Responses$PurchaseSummary(p.id, p.orderId, p.purchaseDate, "
            + "count(pi), coalesce(sum(pi.quantity), 0L)) "
            + "from Purchase p left join p.items pi "
            + "group by p.id, p.orderId, p.purchaseDate order by p.id desc")
    List<PurchaseSummary> findSummaries();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.orderId = :orderId")
    Optional<Purchase> lockByOrderId(@Param("orderId") String orderId);
}
