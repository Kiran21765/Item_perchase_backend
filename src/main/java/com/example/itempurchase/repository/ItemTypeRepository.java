package com.example.itempurchase.repository;

import com.example.itempurchase.dto.Responses.ItemTypeResponse;
import com.example.itempurchase.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemTypeRepository extends JpaRepository<ItemType, Long> {

    Optional<ItemType> findByTypeNameIgnoreCase(String typeName);

    // LEFT JOIN: every type with the number of items that use it
    @Query("select new com.example.itempurchase.dto.Responses$ItemTypeResponse(t.id, t.typeName, count(i)) "
            + "from ItemType t left join t.items i group by t.id, t.typeName order by t.typeName")
    List<ItemTypeResponse> findAllWithCount();
}
