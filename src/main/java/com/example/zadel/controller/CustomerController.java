// CustomerController.java — ПОЛНЫЙ ФАЙЛ для Zadel
package com.example.zadel.controller;

import com.example.zadel.dto.*;
import com.example.zadel.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ==================== Генерация кода ====================

    @GetMapping("/generate")
    public ResponseEntity<CustomerCreateResponse> generate() {
        return ResponseEntity.ok(customerService.generateCode());
    }

    // ==================== Сохранение ====================

    @PostMapping("/draft")
    public ResponseEntity<Void> saveDraft(@RequestBody CustomerSaveRequest request) {
        customerService.saveDraft(request);
        return ResponseEntity.ok().build();
    }

    // ==================== Получение ====================

    @GetMapping
    public ResponseEntity<List<SprCustomerDTO>> getAll() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{uid}")
    public ResponseEntity<SprCustomerDTO> getCustomer(@PathVariable UUID uid) {
        return ResponseEntity.ok(customerService.getCustomer(uid));
    }

    // ==================== Удаление ====================

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID uid) {
        customerService.deleteCustomer(uid);
        return ResponseEntity.ok().build();
    }

    // ==================== ИЗОБРАЖЕНИЯ ====================

    @GetMapping("/{customerUid}/images")
    public ResponseEntity<List<CustomerMediaDTO>> getImages(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getImages(customerUid));
    }

    @PostMapping("/{customerUid}/images")
    public ResponseEntity<CustomerMediaDTO> uploadImage(
            @PathVariable UUID customerUid,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(customerService.uploadImage(customerUid, file));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/images/{uid}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID uid) {
        customerService.deleteImage(uid);
        return ResponseEntity.ok().build();
    }

    // ==================== ДОКУМЕНТЫ ====================

    @GetMapping("/{customerUid}/documents")
    public ResponseEntity<List<CustomerDocumentDTO>> getDocuments(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getDocuments(customerUid));
    }

    @PostMapping("/{customerUid}/documents")
    public ResponseEntity<CustomerDocumentDTO> uploadDocument(
            @PathVariable UUID customerUid,
            @RequestParam("documentName") String documentName,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(customerService.uploadDocument(customerUid, documentName, file));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/documents/{uid}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID uid) {
        customerService.deleteDocument(uid);
        return ResponseEntity.ok().build();
    }

    // ==================== РЕЙТИНГ ====================

    @GetMapping("/{customerUid}/ratings")
    public ResponseEntity<List<CustomerRatingDTO>> getRatings(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getRatings(customerUid));
    }

    @GetMapping("/{customerUid}/ratings/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getAverageRating(customerUid));
    }

    @PostMapping("/{customerUid}/ratings")
    public ResponseEntity<CustomerRatingDTO> addRating(
            @PathVariable UUID customerUid,
            @RequestBody AddCustomerRatingRequest request) {
        return ResponseEntity.ok(customerService.addRating(customerUid, request));
    }

    @DeleteMapping("/ratings/{uid}")
    public ResponseEntity<Void> deleteRating(@PathVariable UUID uid) {
        customerService.deleteRating(uid);
        return ResponseEntity.ok().build();
    }

    // ==================== ИНТЕГРАЦИЯ ====================

    @GetMapping("/{customerUid}/integrations")
    public ResponseEntity<List<CustomerIntegrationDTO>> getIntegrations(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getIntegrations(customerUid));
    }

    @PostMapping("/{customerUid}/integrations")
    public ResponseEntity<CustomerIntegrationDTO> addIntegration(
            @PathVariable UUID customerUid,
            @RequestBody CreateCustomerIntegrationRequest request) {
        return ResponseEntity.ok(customerService.addIntegration(customerUid, request));
    }

    @DeleteMapping("/integrations/{uid}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable UUID uid) {
        customerService.deleteIntegration(uid);
        return ResponseEntity.ok().build();
    }

    // ==================== ТИПЫ ОПИСАНИЙ ====================

    @GetMapping("/description-types")
    public ResponseEntity<List<CustomerDescriptionTypeDTO>> getDescriptionTypes() {
        return ResponseEntity.ok(customerService.getDescriptionTypes());
    }

    // ==================== ЖУРНАЛ СОБЫТИЙ ====================

    @GetMapping("/{customerUid}/events")
    public ResponseEntity<List<CustomerEventLogDTO>> getEvents(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getEvents(customerUid));
    }

    // ==================== ЗАКАЗЫ ЗАКАЗЧИКА ====================

    @GetMapping("/{customerUid}/orders")
    public ResponseEntity<List<MaterialSupplyDTO>> getOrders(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getOrders(customerUid));
    }

    // ==================== АССОРТИМЕНТ ====================

    @GetMapping("/{customerUid}/assortment")
    public ResponseEntity<List<MaterialItemDTO>> getAssortment(@PathVariable UUID customerUid) {
        return ResponseEntity.ok(customerService.getAssortment(customerUid));
    }
}