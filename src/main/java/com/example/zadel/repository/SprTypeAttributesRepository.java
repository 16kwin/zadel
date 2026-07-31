// ==================== НОВЫЙ ФАЙЛ: SprTypeAttributesRepository.java ====================
package com.example.zadel.repository;

import com.example.zadel.model.SprTypeAttributes;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SprTypeAttributesRepository extends JpaRepository<SprTypeAttributes, UUID> {
}