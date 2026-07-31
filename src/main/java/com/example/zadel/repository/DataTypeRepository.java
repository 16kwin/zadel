package com.example.zadel.repository;

import com.example.zadel.model.DataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Репозиторий для работы с типами данных атрибутов */
@Repository
public interface DataTypeRepository extends JpaRepository<DataType, UUID> {
}