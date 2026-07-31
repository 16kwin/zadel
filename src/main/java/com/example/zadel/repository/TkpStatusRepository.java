// ЗАДЕЛ — repository/TkpStatusRepository.java
package com.example.zadel.repository;

import com.example.zadel.model.TkpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TkpStatusRepository extends JpaRepository<TkpStatus, Long> {
    List<TkpStatus> findByTkpUidOrderByDatetimeDesc(String tkpUid);
}