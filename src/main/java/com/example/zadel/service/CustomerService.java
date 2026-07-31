package com.example.zadel.service;

import com.example.zadel.dto.*;
import com.example.zadel.model.*;
import com.example.zadel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final SprCustomerRepository customerRepository;
    private final SprCountryRepository countryRepository;
    private final SprBrandRepository brandRepository;
    private final SprCustomerDescriptionTypeRepository descriptionTypeRepository;
    private final SprCustomerImageRepository imageRepository;
    private final SprCustomerDocumentRepository documentRepository;
    private final RegCustomerRatingRepository ratingRepository;
    private final RegCustomerIntegrationRepository integrationRepository;
    private final RegCustomerEventLogRepository eventLogRepository;
    private final RegCustomerRepository regCustomerRepository;
    private final SprMaterialRepository materialRepository;

    private static final String CUSTOMER_UPLOAD_DIR = "uploads/customers/";

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФАЙЛОВ ====================

    private Path getCustomerDir(UUID customerUid) throws IOException {
        Path dir = Path.of(CUSTOMER_UPLOAD_DIR, customerUid.toString());
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String saveFile(UUID customerUid, MultipartFile file) throws IOException {
        Path dir = getCustomerDir(customerUid);
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = dir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void deleteFile(UUID customerUid, String fileName) {
        try {
            Path filePath = Path.of(CUSTOMER_UPLOAD_DIR, customerUid.toString(), fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getFileUrl(UUID customerUid, String filePath) {
        return "/uploads/customers/" + customerUid + "/" + filePath;
    }

    // ==================== Генерация кода ====================

    public CustomerCreateResponse generateCode() {
        Integer maxCode = customerRepository.findMaxCode();
        Integer code = maxCode + 1;
        return new CustomerCreateResponse(UUID.randomUUID(), code);
    }

    // ==================== Получение заказчика ====================

    public SprCustomerDTO getCustomer(UUID uid) {
        SprCustomer customer = customerRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("Заказчик не найден: " + uid));
        return toDTO(customer);
    }

    // ==================== Получение всех заказчиков ====================

    public List<SprCustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==================== ЛОГИРОВАНИЕ СОБЫТИЙ ====================

    @Transactional
    public void logEvent(UUID customerUid, String eventType, String description,
                         String fieldName, String oldValue, String newValue, String author) {
        SprCustomer customer = customerRepository.findById(customerUid).orElse(null);
        if (customer == null) return;

        RegCustomerEventLog log = RegCustomerEventLog.builder()
                .uid(UUID.randomUUID())
                .customer(customer)
                .eventType(eventType)
                .eventDescription(description)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .author(author)
                .source("Через карточку")
                .createdAt(LocalDateTime.now())
                .build();
        eventLogRepository.save(log);
    }

    public List<CustomerEventLogDTO> getEvents(UUID customerUid) {
        return eventLogRepository.findByCustomerUidOrderByCreatedAtDesc(customerUid).stream()
                .map(e -> CustomerEventLogDTO.builder()
                        .uid(e.getUid())
                        .customerUid(e.getCustomer().getUid())
                        .eventType(e.getEventType())
                        .eventDescription(e.getEventDescription())
                        .fieldName(e.getFieldName())
                        .oldValue(e.getOldValue())
                        .newValue(e.getNewValue())
                        .author(e.getAuthor())
                        .source(e.getSource())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void logFieldChange(UUID customerUid, String fieldName, String oldValue, String newValue, String author) {
        if (oldValue == null && newValue == null) return;
        if (oldValue != null && oldValue.equals(newValue)) return;
        if (oldValue == null && newValue != null) {
            logEvent(customerUid, "UPDATE", "Значение поля '" + fieldName + "' установлено: " + newValue,
                    fieldName, null, newValue, author);
        } else if (newValue == null && oldValue != null) {
            logEvent(customerUid, "UPDATE", "Значение поля '" + fieldName + "' очищено",
                    fieldName, oldValue, null, author);
        } else {
            logEvent(customerUid, "UPDATE", "Значение поля '" + fieldName + "' изменено с '" + oldValue + "' на '" + newValue + "'",
                    fieldName, oldValue, newValue, author);
        }
    }

    // ==================== Сохранение ====================

    @Transactional
    public void saveDraft(CustomerSaveRequest request) {
        SprCustomer customer = customerRepository.findById(request.getUid())
                .orElseGet(() -> {
                    SprCustomer newCustomer = new SprCustomer();
                    newCustomer.setUid(request.getUid());
                    if (request.getCode() == null) {
                        Integer maxCode = customerRepository.findMaxCode();
                        newCustomer.setCode(maxCode != null ? maxCode + 1 : 1);
                    } else {
                        newCustomer.setCode(request.getCode());
                    }
                    return newCustomer;
                });

        if (customer.getCode() == null) {
            Integer maxCode = customerRepository.findMaxCode();
            customer.setCode(maxCode != null ? maxCode + 1 : 1);
        }

        boolean isNewCustomer = customer.getName() == null;
        String author = request.getAuthor() != null ? request.getAuthor() : "Система";

        if (!isNewCustomer) {
            logFieldChange(customer.getUid(), "Наименование", customer.getName(), request.getName(), author);
            logFieldChange(customer.getUid(), "Адрес", customer.getAddress(), request.getAddress(), author);
            logFieldChange(customer.getUid(), "Описание", customer.getDescription(), request.getDescription(), author);
            logFieldChange(customer.getUid(), "Email", customer.getEmail(), request.getEmail(), author);
            logFieldChange(customer.getUid(), "Сайт", customer.getWebsite(), request.getWebsite(), author);
            logFieldChange(customer.getUid(), "Телефон", customer.getPhone(), request.getPhone(), author);
            logFieldChange(customer.getUid(), "ИНН", customer.getInn(), request.getInn(), author);
            logFieldChange(customer.getUid(), "ОГРН", customer.getOgrn(), request.getOgrn(), author);
            logFieldChange(customer.getUid(), "КПП", customer.getKpp(), request.getKpp(), author);
            logFieldChange(customer.getUid(), "Контактное лицо", customer.getContactPerson(), request.getContactPerson(), author);
            logFieldChange(customer.getUid(), "Должность контактного лица", customer.getContactPosition(), request.getContactPosition(), author);
            logFieldChange(customer.getUid(), "Телефон контактного лица", customer.getContactPhone(), request.getContactPhone(), author);
            logFieldChange(customer.getUid(), "Руководитель", customer.getDirector(), request.getDirector(), author);
            logFieldChange(customer.getUid(), "Должность руководителя", customer.getDirectorPosition(), request.getDirectorPosition(), author);
            logFieldChange(customer.getUid(), "Банк", customer.getBankName(), request.getBankName(), author);
            logFieldChange(customer.getUid(), "БИК", customer.getBik(), request.getBik(), author);
            logFieldChange(customer.getUid(), "Корр. счет", customer.getCorrespondentAccount(), request.getCorrespondentAccount(), author);
            logFieldChange(customer.getUid(), "Расч. счет", customer.getSettlementAccount(), request.getSettlementAccount(), author);
        }

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setDescription(request.getDescription());
        customer.setEmail(request.getEmail());
        customer.setWebsite(request.getWebsite());
        customer.setPhone(request.getPhone());
        customer.setInn(request.getInn());
        customer.setOgrn(request.getOgrn());
        customer.setKpp(request.getKpp());
        customer.setContactPerson(request.getContactPerson());
        customer.setContactPosition(request.getContactPosition());
        customer.setContactPhone(request.getContactPhone());
        customer.setDirector(request.getDirector());
        customer.setDirectorPosition(request.getDirectorPosition());
        customer.setBankName(request.getBankName());
        customer.setBik(request.getBik());
        customer.setCorrespondentAccount(request.getCorrespondentAccount());
        customer.setSettlementAccount(request.getSettlementAccount());

        if (request.getCountryUid() != null) {
            customer.setCountry(countryRepository.findById(request.getCountryUid()).orElse(null));
        }
        if (request.getBrandUid() != null) {
            customer.setBrand(brandRepository.findById(request.getBrandUid()).orElse(null));
        }
        if (request.getShortDescriptionUid() != null) {
            customer.setShortDescription(descriptionTypeRepository.findById(request.getShortDescriptionUid()).orElse(null));
        }

        customerRepository.save(customer);

        if (isNewCustomer) {
            logEvent(customer.getUid(), "CREATE", "Создание карточки заказчика", null, null, null, author);
        }
    }

    // ==================== Удаление ====================

    @Transactional
    public void deleteCustomer(UUID uid) {
        deleteAllCustomerMedia(uid);
        customerRepository.deleteById(uid);
    }

    // ==================== ИЗОБРАЖЕНИЯ ====================

    public List<CustomerMediaDTO> getImages(UUID customerUid) {
        return imageRepository.findByCustomerUidOrderBySortOrderAsc(customerUid).stream()
                .map(img -> new CustomerMediaDTO(
                        img.getUid(),
                        img.getCustomer().getUid(),
                        img.getFilePath(),
                        img.getOriginalName(),
                        getFileUrl(customerUid, img.getFilePath()),
                        img.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerMediaDTO uploadImage(UUID customerUid, MultipartFile file) throws IOException {
        String fileName = saveFile(customerUid, file);
        SprCustomer customer = customerRepository.findById(customerUid)
                .orElseThrow(() -> new RuntimeException("Заказчик не найден: " + customerUid));
        long count = imageRepository.findByCustomerUidOrderBySortOrderAsc(customerUid).size();
        int nextSortOrder = (int) count;
        SprCustomerImage image = new SprCustomerImage();
        image.setUid(UUID.randomUUID());
        image.setCustomer(customer);
        image.setFilePath(fileName);
        image.setOriginalName(file.getOriginalFilename());
        image.setSortOrder(nextSortOrder);
        imageRepository.save(image);
        logEvent(customerUid, "ADD", "Добавлено изображение '" + file.getOriginalFilename() + "'",
                "Изображение", null, file.getOriginalFilename(), "Система");
        return new CustomerMediaDTO(image.getUid(), customerUid, fileName, file.getOriginalFilename(),
                getFileUrl(customerUid, fileName), nextSortOrder);
    }

    @Transactional
    public void deleteImage(UUID uid) {
        SprCustomerImage image = imageRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("Изображение не найдено: " + uid));
        UUID customerUid = image.getCustomer().getUid();
        String fileName = image.getOriginalName();
        deleteFile(customerUid, image.getFilePath());
        imageRepository.delete(image);
        logEvent(customerUid, "DELETE", "Удалено изображение '" + fileName + "'",
                "Изображение", fileName, null, "Система");
    }

    // ==================== ДОКУМЕНТЫ ====================

    public List<CustomerDocumentDTO> getDocuments(UUID customerUid) {
        return documentRepository.findByCustomerUidOrderByCreatedAtDesc(customerUid).stream()
                .map(doc -> new CustomerDocumentDTO(doc.getUid(), doc.getCustomer().getUid(),
                        doc.getDocumentName(), doc.getFilePath(), doc.getOriginalName(),
                        getFileUrl(customerUid, doc.getFilePath()), doc.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerDocumentDTO uploadDocument(UUID customerUid, String documentName, MultipartFile file) throws IOException {
        String fileName = saveFile(customerUid, file);
        SprCustomer customer = customerRepository.findById(customerUid)
                .orElseThrow(() -> new RuntimeException("Заказчик не найден: " + customerUid));
        SprCustomerDocument document = new SprCustomerDocument();
        document.setUid(UUID.randomUUID());
        document.setCustomer(customer);
        document.setDocumentName(documentName);
        document.setFilePath(fileName);
        document.setOriginalName(file.getOriginalFilename());
        documentRepository.save(document);
        logEvent(customerUid, "ADD", "Добавлен документ '" + documentName + "'",
                "Документ", null, documentName, "Система");
        return new CustomerDocumentDTO(document.getUid(), customerUid, document.getDocumentName(),
                document.getFilePath(), document.getOriginalName(),
                getFileUrl(customerUid, fileName), document.getCreatedAt());
    }

    @Transactional
    public void deleteDocument(UUID uid) {
        SprCustomerDocument document = documentRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("Документ не найден: " + uid));
        UUID customerUid = document.getCustomer().getUid();
        String docName = document.getDocumentName();
        deleteFile(customerUid, document.getFilePath());
        documentRepository.delete(document);
        logEvent(customerUid, "DELETE", "Удален документ '" + docName + "'",
                "Документ", docName, null, "Система");
    }

    // ==================== РЕЙТИНГ ====================

    public List<CustomerRatingDTO> getRatings(UUID customerUid) {
        return ratingRepository.findByCustomerUidOrderByCreatedAtDesc(customerUid).stream()
                .map(r -> new CustomerRatingDTO(r.getUid(), r.getCustomer().getUid(), r.getRating(),
                        r.getComment(), r.getAuthor(), r.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Double getAverageRating(UUID customerUid) {
        return ratingRepository.getAverageRatingByCustomerUid(customerUid);
    }

    @Transactional
    public CustomerRatingDTO addRating(UUID customerUid, AddCustomerRatingRequest request) {
        SprCustomer customer = customerRepository.findById(customerUid)
                .orElseThrow(() -> new RuntimeException("Заказчик не найден: " + customerUid));
        RegCustomerRating rating = new RegCustomerRating();
        rating.setUid(UUID.randomUUID());
        rating.setCustomer(customer);
        rating.setRating(request.getRating());
        rating.setComment(request.getComment());
        rating.setAuthor(request.getAuthor());
        ratingRepository.save(rating);
        logEvent(customerUid, "ADD", "Добавлен отзыв от '" + request.getAuthor() + "': " + request.getRating() + " звезд",
                "Рейтинг", null, request.getRating().toString(), request.getAuthor());
        return new CustomerRatingDTO(rating.getUid(), customerUid, rating.getRating(),
                rating.getComment(), rating.getAuthor(), rating.getCreatedAt());
    }

    @Transactional
    public void deleteRating(UUID ratingUid) {
        RegCustomerRating rating = ratingRepository.findById(ratingUid).orElse(null);
        if (rating != null) {
            logEvent(rating.getCustomer().getUid(), "DELETE", "Удален отзыв от '" + rating.getAuthor() + "'",
                    "Рейтинг", rating.getRating().toString(), null, rating.getAuthor());
        }
        ratingRepository.deleteById(ratingUid);
    }

    // ==================== ИНТЕГРАЦИЯ ====================

    public List<CustomerIntegrationDTO> getIntegrations(UUID customerUid) {
        return integrationRepository.findByCustomerUidOrderByCreatedAtDesc(customerUid).stream()
                .map(i -> new CustomerIntegrationDTO(i.getUid(), i.getCustomer().getUid(), i.getEvent(),
                        i.getExchangeType(), i.getDirection(), i.getProtocol(), i.getTargetSystem(), i.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerIntegrationDTO addIntegration(UUID customerUid, CreateCustomerIntegrationRequest request) {
        SprCustomer customer = customerRepository.findById(customerUid)
                .orElseThrow(() -> new RuntimeException("Заказчик не найден: " + customerUid));
        RegCustomerIntegration integration = new RegCustomerIntegration();
        integration.setUid(UUID.randomUUID());
        integration.setCustomer(customer);
        integration.setEvent("Объект синхронизирован");
        integration.setExchangeType(request.getExchangeType());
        integration.setDirection(request.getDirection());
        integration.setProtocol(request.getProtocol());
        integration.setTargetSystem(request.getTargetSystem());
        integrationRepository.save(integration);
        return new CustomerIntegrationDTO(integration.getUid(), customerUid, integration.getEvent(),
                integration.getExchangeType(), integration.getDirection(), integration.getProtocol(),
                integration.getTargetSystem(), integration.getCreatedAt());
    }

    @Transactional
    public void deleteIntegration(UUID integrationUid) {
        integrationRepository.deleteById(integrationUid);
    }

    // ==================== ТИПЫ ОПИСАНИЙ ====================

    public List<CustomerDescriptionTypeDTO> getDescriptionTypes() {
        return descriptionTypeRepository.findAll().stream()
                .map(t -> new CustomerDescriptionTypeDTO(t.getUid(), t.getName()))
                .collect(Collectors.toList());
    }

    // ==================== ЗАКАЗЫ ЗАКАЗЧИКА ====================

    public List<MaterialSupplyDTO> getOrders(UUID customerUid) {
        return regCustomerRepository.findByCustomerUid(customerUid).stream()
                .map(r -> new MaterialSupplyDTO(
                        r.getUid(),
                        r.getMaterial() != null ? r.getMaterial().getUid() : null,
                        r.getMaterial() != null ? r.getMaterial().getNameMaterial() : null,
                        r.getCustomer() != null ? r.getCustomer().getUid() : null,
                        r.getCustomer() != null ? r.getCustomer().getName() : null,
                        r.getSupplyDate(),
                        r.getDocumentName(),
                        r.getFilePath(),
                        r.getOriginalName(),
                        r.getFilePath() != null ? getFileUrl(customerUid, r.getFilePath()) : null))
                .collect(Collectors.toList());
    }

    // ==================== АССОРТИМЕНТ ====================

    public List<MaterialItemDTO> getAssortment(UUID customerUid) {
        List<RegCustomers> supplies = regCustomerRepository.findByCustomerUid(customerUid);
        return supplies.stream()
                .filter(s -> s.getMaterial() != null)
                .map(s -> {
                    SprMaterial m = s.getMaterial();
                    MaterialItemDTO item = new MaterialItemDTO();
                    item.setUid(m.getUid());
                    item.setName(m.getNameMaterial());
                    item.setArticle(m.getArticle());
                    item.setCode(m.getCodeMaterial());
                    item.setTypeMainName(m.getTypeMain() != null ? m.getTypeMain().getTypeName() : null);
                    return item;
                })
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== Удаление всех медиа ====================

    @Transactional
    public void deleteAllCustomerMedia(UUID customerUid) {
        imageRepository.deleteByCustomerUid(customerUid);
        documentRepository.deleteByCustomerUid(customerUid);
        ratingRepository.deleteByCustomerUid(customerUid);
        integrationRepository.deleteByCustomerUid(customerUid);
        try {
            Path dir = Path.of(CUSTOMER_UPLOAD_DIR, customerUid.toString());
            if (Files.exists(dir)) {
                try (var files = Files.list(dir)) {
                    files.forEach(f -> { try { Files.deleteIfExists(f); } catch (IOException ignored) {} });
                }
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ==================== DTO конвертер ====================

    private SprCustomerDTO toDTO(SprCustomer c) {
        return SprCustomerDTO.builder()
                .uid(c.getUid()).code(c.getCode()).name(c.getName())
                .countryUid(c.getCountry() != null ? c.getCountry().getUid() : null)
                .countryName(c.getCountry() != null ? c.getCountry().getName() : null)
                .address(c.getAddress())
                .shortDescriptionUid(c.getShortDescription() != null ? c.getShortDescription().getUid() : null)
                .shortDescriptionName(c.getShortDescription() != null ? c.getShortDescription().getName() : null)
                .description(c.getDescription()).email(c.getEmail()).website(c.getWebsite()).phone(c.getPhone())
                .brandUid(c.getBrand() != null ? c.getBrand().getUid() : null)
                .brandName(c.getBrand() != null ? c.getBrand().getName() : null)
                .inn(c.getInn()).ogrn(c.getOgrn()).kpp(c.getKpp())
                .contactPerson(c.getContactPerson()).contactPosition(c.getContactPosition()).contactPhone(c.getContactPhone())
                .director(c.getDirector()).directorPosition(c.getDirectorPosition())
                .bankName(c.getBankName()).bik(c.getBik())
                .correspondentAccount(c.getCorrespondentAccount()).settlementAccount(c.getSettlementAccount())
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .build();
    }
}