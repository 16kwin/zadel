package com.example.zadel.repository;

import com.example.zadel.model.SprTypePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Репозиторий для справочника "Группы номенклатуры" */
@Repository
public interface SprTypePurposeRepository extends JpaRepository<SprTypePurpose, UUID> {
    
    /** Найти все группы номенклатуры, относящиеся к указанной группе учета */
    List<SprTypePurpose> findByTypeMaterialUid(UUID typeMaterialUid);
}