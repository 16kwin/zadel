// NomenclatureController.java — ПОЛНЫЙ ФАЙЛ для Zadel
package com.example.zadel.controller;

import com.example.zadel.dto.*;
import com.example.zadel.service.NomenclatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nomenclature")
@RequiredArgsConstructor
public class NomenclatureController {

    private final NomenclatureService nomenclatureService;

    // ==================== Дерево каталога ====================

    @GetMapping("/tree")
    public ResponseEntity<List<GroupMaterialTreeDTO>> getTree() {
        return ResponseEntity.ok(nomenclatureService.getFullTree());
    }

    // ==================== Группы учета ====================

    @GetMapping("/type-materials")
    public ResponseEntity<List<SprTypeMaterialDTO>> getTypeMaterials() {
        return ResponseEntity.ok(nomenclatureService.getTypeMaterials());
    }

    // ==================== Группы номенклатуры ====================

    @GetMapping("/type-purposes")
    public ResponseEntity<List<SprTypePurposeDTO>> getTypePurposes(
            @RequestParam(required = false) UUID typeMaterialUid) {
        if (typeMaterialUid != null) {
            return ResponseEntity.ok(nomenclatureService.getTypePurposes(typeMaterialUid));
        }
        return ResponseEntity.ok(nomenclatureService.getAllTypePurposes());
    }

    // ==================== Виды номенклатуры ====================

    @GetMapping("/type-products")
    public ResponseEntity<List<SprTypeProductDTO>> getTypeProducts(
            @RequestParam(required = false) UUID typePurposeUid) {
        if (typePurposeUid != null) {
            return ResponseEntity.ok(nomenclatureService.getTypeProducts(typePurposeUid));
        }
        return ResponseEntity.ok(nomenclatureService.getAllTypeProducts());
    }

    // ==================== Виды характеристик ====================

    @GetMapping("/type-attributes")
    public ResponseEntity<List<SprTypeAttributeDTO>> getTypeAttributes() {
        return ResponseEntity.ok(nomenclatureService.getTypeAttributes());
    }

    // ==================== Единицы измерения ====================

    @GetMapping("/measures")
    public ResponseEntity<List<SprMeasureDTO>> getMeasures() {
        return ResponseEntity.ok(nomenclatureService.getMeasures());
    }

    // ==================== Производители ====================

    @GetMapping("/manufacturers")
    public ResponseEntity<List<SprManufacturerDTO>> getManufacturers() {
        return ResponseEntity.ok(nomenclatureService.getManufacturers());
    }

    // ==================== Бренды ====================

    @GetMapping("/brands")
    public ResponseEntity<List<SprBrandDTO>> getBrands(
            @RequestParam(required = false) UUID manufacturerUid) {
        return ResponseEntity.ok(nomenclatureService.getBrands(manufacturerUid));
    }

    // ==================== Модели ====================

    @GetMapping("/models")
    public ResponseEntity<List<SprModelOfBrandDTO>> getModels(
            @RequestParam(required = false) UUID brandUid) {
        return ResponseEntity.ok(nomenclatureService.getModels(brandUid));
    }

    // ==================== Страны ====================

    @GetMapping("/countries")
    public ResponseEntity<List<SprCountryDTO>> getCountries() {
        return ResponseEntity.ok(nomenclatureService.getCountries());
    }

    // ==================== ИЗОБРАЖЕНИЯ ====================

    @GetMapping("/{materialUid}/images")
    public ResponseEntity<List<MaterialMediaDTO>> getImages(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getImages(materialUid));
    }

    // ==================== ЧЕРТЕЖИ ====================

    @GetMapping("/{materialUid}/blueprints")
    public ResponseEntity<List<MaterialMediaDTO>> getBlueprints(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getBlueprints(materialUid));
    }

    // ==================== ХАРАКТЕРИСТИКИ ====================

    @GetMapping("/{materialUid}/characteristics")
    public ResponseEntity<List<MaterialCharacteristicDTO>> getCharacteristics(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getCharacteristics(materialUid));
    }

    // ==================== АНАЛОГИ ====================

    @GetMapping("/{materialUid}/analogs")
    public ResponseEntity<List<MaterialAnalogDTO>> getAnalogs(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getAnalogs(materialUid));
    }

    // ==================== РЕЙТИНГ ====================

    @GetMapping("/{materialUid}/ratings")
    public ResponseEntity<List<MaterialRatingDTO>> getRatings(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getRatings(materialUid));
    }

    @GetMapping("/{materialUid}/ratings/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getAverageRating(materialUid));
    }

    // ==================== ИНТЕГРАЦИЯ ====================

    @GetMapping("/{materialUid}/integrations")
    public ResponseEntity<List<MaterialIntegrationDTO>> getIntegrations(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getIntegrations(materialUid));
    }

    // ==================== ЦЕНЫ ====================

    @GetMapping("/{materialUid}/prices")
    public ResponseEntity<List<MaterialPriceDTO>> getPrices(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getPrices(materialUid));
    }

    // ==================== ДОКУМЕНТЫ ====================

    @GetMapping("/{materialUid}/documents")
    public ResponseEntity<List<MaterialDocumentDTO>> getDocuments(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getDocuments(materialUid));
    }

    // ==================== ЖУРНАЛ СОБЫТИЙ ====================

    @GetMapping("/{materialUid}/events")
    public ResponseEntity<List<EventLogDTO>> getEvents(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getEvents(materialUid));
    }

    // ==================== ЗАКАЗЧИКИ ====================

    @GetMapping("/customers")
    public ResponseEntity<List<SprCustomerDTO>> getCustomers() {
        return ResponseEntity.ok(nomenclatureService.getCustomers());
    }

    @GetMapping("/customers/{uid}")
    public ResponseEntity<SprCustomerDTO> getCustomer(@PathVariable UUID uid) {
        return ResponseEntity.ok(nomenclatureService.getCustomer(uid));
    }

    // ==================== ПРИВЯЗКА ЗАКАЗЧИКОВ К МАТЕРИАЛУ ====================

    @GetMapping("/{materialUid}/supply")
    public ResponseEntity<List<MaterialSupplyDTO>> getMaterialCustomers(@PathVariable UUID materialUid) {
        return ResponseEntity.ok(nomenclatureService.getMaterialCustomers(materialUid));
    }

    // ==================== Получение одного материала ====================
    // ВАЖНО: этот метод должен быть ПОСЛЕ всех /{materialUid}/...

    @GetMapping("/{uid}")
    public ResponseEntity<SprMaterialDTO> getMaterial(@PathVariable UUID uid) {
        return ResponseEntity.ok(nomenclatureService.getMaterial(uid));
    }
}