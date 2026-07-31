// ЗАДЕЛ — repository/TkpListRepository.java
package com.example.zadel.repository;

import com.example.zadel.model.TkpList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TkpListRepository extends JpaRepository<TkpList, String> {
    List<TkpList> findByStatus(String status);
    List<TkpList> findByOrderUid(String orderUid);
}