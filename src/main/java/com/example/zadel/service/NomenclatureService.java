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
public class NomenclatureService {

    private final SprMaterialRepository materialRepository;
    private final RegGroupMaterialRepository groupMaterialRepository;
    private final SprTypeMaterialRepository typeMaterialRepository;
    private final SprTypePurposeRepository typePurposeRepository;
    private final SprTypeProductRepository typeProductRepository;
    private final SprMeasureRepository measureRepository;
    private final SprManufacturerRepository manufacturerRepository;
    private final SprBrandRepository brandRepository;
    private final SprModelOfBrandRepository modelOfBrandRepository;
    private final SprCountryRepository countryRepository;
    private final SprMaterialImageRepository imageRepository;
    private final SprMaterialBlueprintRepository blueprintRepository;
    private final SprMaterialCodeRepository codeRepository;
    private final SprMaterialDocumentRepository documentRepository;
    private final RegPriceRepository priceRepository;
    private final CustomerService customerService;
    private final DocEntranceRepository docEntranceRepository;
    private final RegAttributesRepository regAttributesRepository;
    private final SprTypeAttributesRepository typeAttributesRepository;
    private final RegCustomerRepository regCustomerRepository;
    private final RegAnalogRepository regAnalogRepository;
    private final RegRatingRepository regRatingRepository;
    private final RegIntegrationRepository regIntegrationRepository;
    private final RegEventLogRepository eventLogRepository;

    private static final String NOMENCLATURE_UPLOAD_DIR = "uploads/nomenclature/";

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФАЙЛОВ ====================

    private Path getMaterialDir(UUID materialUid) throws IOException {
        Path dir = Path.of(NOMENCLATURE_UPLOAD_DIR, materialUid.toString());
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String saveFile(UUID materialUid, MultipartFile file) throws IOException {
        Path dir = getMaterialDir(materialUid);
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = dir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void deleteFile(UUID materialUid, String fileName) {
        try {
            Path filePath = Path.of(NOMENCLATURE_UPLOAD_DIR, materialUid.toString(), fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getFileUrl(UUID materialUid, String filePath) {
        return "/uploads/nomenclature/" + materialUid + "/" + filePath;
    }

    // ==================== ЛОГИРОВАНИЕ СОБЫТИЙ ====================

    @Transactional
    public void logEvent(UUID materialUid, String eventType, String description,
                         String fieldName, String oldValue, String newValue, String author) {
        SprMaterial material = materialRepository.findById(materialUid).orElse(null);
        if (material == null) return;

        RegEventLog log = RegEventLog.builder()
                .uid(UUID.randomUUID())
                .material(material)
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

    public List<EventLogDTO> getEvents(UUID materialUid) {
        return eventLogRepository.findByMaterialUidOrderByCreatedAtDesc(materialUid).stream()
                .map(e -> EventLogDTO.builder()
                        .uid(e.getUid())
                        .materialUid(e.getMaterial().getUid())
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

    // ==================== Получение материала ====================

    public SprMaterialDTO getMaterial(UUID uid) {
        SprMaterial material = materialRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("Материал не найден: " + uid));

        SprMaterialDTO dto = new SprMaterialDTO();
        dto.setUid(material.getUid());
        dto.setCode(material.getCodeMaterial());
        dto.setName(material.getNameMaterial());
        dto.setArticle(material.getArticle());
        dto.setDescription(material.getDescription());
        dto.setUsage(material.getUsage());
        dto.setWasteMaterial(material.getWasteMaterial());
        dto.setRecycleMaterial(material.getRecycleMaterial());

        if (material.getGroupMaterial() != null) {
            dto.setGroupUid(material.getGroupMaterial().getUid());
            dto.setGroupName(material.getGroupMaterial().getGroupName());
        }
        if (material.getTypeMain() != null) {
            dto.setTypeMainUid(material.getTypeMain().getUid());
            dto.setTypeMainName(material.getTypeMain().getTypeName());
        }
        if (material.getTypePurpose() != null) {
            dto.setTypePurposeUid(material.getTypePurpose().getUid());
            dto.setTypePurposeName(material.getTypePurpose().getTypeName());
        }
        if (material.getTypeProduct() != null) {
            dto.setTypeProductUid(material.getTypeProduct().getUid());
            dto.setTypeProductName(material.getTypeProduct().getTypeName());
        }
        if (material.getMeasure() != null) {
            dto.setMeasureUid(material.getMeasure().getUid());
            dto.setMeasureName(material.getMeasure().getName());
        }
        if (material.getManufacturer() != null) {
            dto.setManufacturerUid(material.getManufacturer().getUid());
            dto.setManufacturerName(material.getManufacturer().getName());
        }
        if (material.getBrand() != null) {
            dto.setBrandUid(material.getBrand().getUid());
            dto.setBrandName(material.getBrand().getName());
        }
        if (material.getModelOfBrand() != null) {
            dto.setModelOfBrandUid(material.getModelOfBrand().getUid());
            dto.setModelOfBrandName(material.getModelOfBrand().getName());
        }
        if (material.getCountry() != null) {
            dto.setCountryUid(material.getCountry().getUid());
            dto.setCountryName(material.getCountry().getName());
        }

        return dto;
    }

    // ==================== Дерево ====================

    public List<GroupMaterialTreeDTO> getFullTree() {
        List<RegGroupMaterial> allGroups = groupMaterialRepository.findAll();
        List<RegGroupMaterial> roots = allGroups.stream()
                .filter(g -> g.getParentGroup() == null)
                .collect(Collectors.toList());
        return roots.stream()
                .map(root -> buildFullTree(root, allGroups))
                .collect(Collectors.toList());
    }

    private GroupMaterialTreeDTO buildFullTree(RegGroupMaterial group, List<RegGroupMaterial> allGroups) {
        GroupMaterialTreeDTO dto = new GroupMaterialTreeDTO();
        dto.setUid(group.getUid());
        dto.setName(group.getGroupName());
        dto.setCode(group.getGroupCode());

        List<GroupMaterialTreeDTO> children = allGroups.stream()
                .filter(g -> g.getParentGroup() != null && g.getParentGroup().equals(group.getUid()))
                .map(child -> buildFullTree(child, allGroups))
                .collect(Collectors.toList());
        dto.setChildren(children);

        List<SprMaterial> materials = materialRepository.findByGroupMaterialUid(group.getUid());
        List<MaterialItemDTO> materialItems = materials.stream()
                .map(m -> {
                    MaterialItemDTO item = new MaterialItemDTO();
                    item.setUid(m.getUid());
                    item.setName(m.getNameMaterial());
                    item.setArticle(m.getArticle());
                    item.setCode(m.getCodeMaterial());
                    item.setTypeMainName(m.getTypeMain() != null ? m.getTypeMain().getTypeName() : null);
                    item.setTypePurposeName(m.getTypePurpose() != null ? m.getTypePurpose().getTypeName() : null);
                    item.setTypeProductName(m.getTypeProduct() != null ? m.getTypeProduct().getTypeName() : null);
                    return item;
                })
                .collect(Collectors.toList());
        dto.setMaterials(materialItems);

        return dto;
    }

    // ==================== ХАРАКТЕРИСТИКИ ====================

    public List<MaterialCharacteristicDTO> getCharacteristics(UUID materialUid) {
        List<RegAttributes> attrs = regAttributesRepository.findByMaterialUid(materialUid);
        return attrs.stream()
                .map(a -> {
                    boolean isCustom = a.getAttributeType() == null;
                    return new MaterialCharacteristicDTO(
                            a.getUid(),
                            a.getMaterial() != null ? a.getMaterial().getUid() : materialUid,
                            a.getAttributeType() != null ? a.getAttributeType().getUid() : null,
                            a.getAttributeType() != null ? a.getAttributeType().getName() : null,
                            isCustom ? (a.getMeaning() != null ? a.getMeaning() : "Пользовательская") : null,
                            isCustom ? null : a.getMeaning(),
                            a.getMeasure() != null ? a.getMeasure().getUid() : null,
                            a.getMeasure() != null ? a.getMeasure().getName() : null,
                            isCustom
                    );
                })
                .collect(Collectors.toList());
    }

    // ==================== ВИДЫ ХАРАКТЕРИСТИК ====================

    public List<SprTypeAttributeDTO> getTypeAttributes() {
        return typeAttributesRepository.findAll().stream()
                .map(a -> new SprTypeAttributeDTO(a.getUid(), a.getName(), a.getDesignation()))
                .collect(Collectors.toList());
    }

    // ==================== СПРАВОЧНИКИ ====================

    public List<SprTypeMaterialDTO> getTypeMaterials() {
        return typeMaterialRepository.findAll().stream()
                .map(m -> new SprTypeMaterialDTO(m.getUid(), m.getTypeName()))
                .collect(Collectors.toList());
    }

    public List<SprTypePurposeDTO> getAllTypePurposes() {
        return typePurposeRepository.findAll().stream()
                .map(p -> new SprTypePurposeDTO(
                        p.getUid(), p.getTypeName(),
                        p.getTypeMaterial() != null ? p.getTypeMaterial().getUid() : null,
                        p.getTypeMaterial() != null ? p.getTypeMaterial().getTypeName() : null))
                .collect(Collectors.toList());
    }

    public List<SprTypePurposeDTO> getTypePurposes(UUID typeMaterialUid) {
        return typePurposeRepository.findByTypeMaterialUid(typeMaterialUid).stream()
                .map(p -> new SprTypePurposeDTO(
                        p.getUid(), p.getTypeName(),
                        p.getTypeMaterial() != null ? p.getTypeMaterial().getUid() : null,
                        p.getTypeMaterial() != null ? p.getTypeMaterial().getTypeName() : null))
                .collect(Collectors.toList());
    }

    public List<SprTypeProductDTO> getAllTypeProducts() {
        return typeProductRepository.findAll().stream()
                .map(p -> new SprTypeProductDTO(
                        p.getUid(), p.getTypeName(),
                        p.getTypePurpose() != null ? p.getTypePurpose().getUid() : null,
                        p.getTypePurpose() != null ? p.getTypePurpose().getTypeName() : null,
                        p.getTypePurpose() != null && p.getTypePurpose().getTypeMaterial() != null
                                ? p.getTypePurpose().getTypeMaterial().getTypeName() : null))
                .collect(Collectors.toList());
    }

    public List<SprTypeProductDTO> getTypeProducts(UUID typePurposeUid) {
        return typeProductRepository.findByTypePurposeUid(typePurposeUid).stream()
                .map(p -> new SprTypeProductDTO(
                        p.getUid(), p.getTypeName(),
                        p.getTypePurpose() != null ? p.getTypePurpose().getUid() : null,
                        p.getTypePurpose() != null ? p.getTypePurpose().getTypeName() : null,
                        p.getTypePurpose() != null && p.getTypePurpose().getTypeMaterial() != null
                                ? p.getTypePurpose().getTypeMaterial().getTypeName() : null))
                .collect(Collectors.toList());
    }

    public List<SprMeasureDTO> getMeasures() {
        return measureRepository.findAll().stream()
                .map(m -> new SprMeasureDTO(m.getUid(), m.getName(), m.getDescription()))
                .collect(Collectors.toList());
    }

    public List<SprManufacturerDTO> getManufacturers() {
        return manufacturerRepository.findAll().stream()
                .map(m -> new SprManufacturerDTO(m.getUid(), m.getName(), m.getDescription()))
                .collect(Collectors.toList());
    }

    public List<SprBrandDTO> getBrands(UUID manufacturerUid) {
        if (manufacturerUid != null) {
            return brandRepository.findByManufacturerUid(manufacturerUid).stream()
                    .map(b -> new SprBrandDTO(
                            b.getUid(), b.getName(), b.getDescription(),
                            b.getManufacturer() != null ? b.getManufacturer().getUid() : null,
                            b.getManufacturer() != null ? b.getManufacturer().getName() : null))
                    .collect(Collectors.toList());
        }
        return brandRepository.findAll().stream()
                .map(b -> new SprBrandDTO(
                        b.getUid(), b.getName(), b.getDescription(),
                        b.getManufacturer() != null ? b.getManufacturer().getUid() : null,
                        b.getManufacturer() != null ? b.getManufacturer().getName() : null))
                .collect(Collectors.toList());
    }

    public List<SprModelOfBrandDTO> getModels(UUID brandUid) {
        if (brandUid != null) {
            return modelOfBrandRepository.findByBrandUid(brandUid).stream()
                    .map(m -> new SprModelOfBrandDTO(
                            m.getUid(), m.getName(), m.getDescription(),
                            m.getBrand() != null ? m.getBrand().getUid() : null,
                            m.getBrand() != null ? m.getBrand().getName() : null,
                            m.getBrand() != null && m.getBrand().getManufacturer() != null
                                    ? m.getBrand().getManufacturer().getName() : null))
                    .collect(Collectors.toList());
        }
        return modelOfBrandRepository.findAll().stream()
                .map(m -> new SprModelOfBrandDTO(
                        m.getUid(), m.getName(), m.getDescription(),
                        m.getBrand() != null ? m.getBrand().getUid() : null,
                        m.getBrand() != null ? m.getBrand().getName() : null,
                        m.getBrand() != null && m.getBrand().getManufacturer() != null
                                ? m.getBrand().getManufacturer().getName() : null))
                .collect(Collectors.toList());
    }

    public List<SprCountryDTO> getCountries() {
        return countryRepository.findAll().stream()
                .map(c -> new SprCountryDTO(c.getUid(), c.getName()))
                .collect(Collectors.toList());
    }

    // ==================== ИЗОБРАЖЕНИЯ ====================

    public List<MaterialMediaDTO> getImages(UUID materialUid) {
        return imageRepository.findByMaterialUidOrderBySortOrderAsc(materialUid).stream()
                .map(img -> new MaterialMediaDTO(
                        img.getUid(),
                        img.getMaterial().getUid(),
                        img.getFilePath(),
                        img.getOriginalName(),
                        getFileUrl(materialUid, img.getFilePath()),
                        img.getSortOrder()))
                .collect(Collectors.toList());
    }

    // ==================== ЧЕРТЕЖИ ====================

    public List<MaterialMediaDTO> getBlueprints(UUID materialUid) {
        return blueprintRepository.findByMaterialUid(materialUid).stream()
                .map(bp -> new MaterialMediaDTO(
                        bp.getUid(),
                        bp.getMaterial().getUid(),
                        bp.getFilePath(),
                        bp.getOriginalName(),
                        getFileUrl(materialUid, bp.getFilePath()),
                        null))
                .collect(Collectors.toList());
    }

    // ==================== ДОКУМЕНТЫ ====================

    public List<MaterialDocumentDTO> getDocuments(UUID materialUid) {
        return documentRepository.findByMaterialUidOrderByCreatedAtDesc(materialUid).stream()
                .map(doc -> new MaterialDocumentDTO(
                        doc.getUid(),
                        doc.getMaterial().getUid(),
                        doc.getDocumentName(),
                        doc.getFilePath(),
                        doc.getOriginalName(),
                        getFileUrl(materialUid, doc.getFilePath()),
                        doc.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ==================== ЗАКАЗЧИКИ ====================

    public List<SprCustomerDTO> getCustomers() {
        return customerService.getAllCustomers();
    }

    public SprCustomerDTO getCustomer(UUID uid) {
        return customerService.getCustomer(uid);
    }

    // ==================== АНАЛОГИ ====================

    public List<MaterialAnalogDTO> getAnalogs(UUID materialUid) {
        return regAnalogRepository.findByMaterialUid(materialUid).stream()
                .map(a -> new MaterialAnalogDTO(
                        a.getUid(),
                        a.getMaterial().getUid(),
                        a.getAnalogMaterial().getUid(),
                        a.getAnalogMaterial().getNameMaterial(),
                        a.getAnalogMaterial().getModelOfBrand() != null ? a.getAnalogMaterial().getModelOfBrand().getName() : null,
                        a.getCompatibilityPercent(),
                        a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ==================== РЕЙТИНГ ====================

    public List<MaterialRatingDTO> getRatings(UUID materialUid) {
        return regRatingRepository.findByMaterialUidOrderByCreatedAtDesc(materialUid).stream()
                .map(r -> new MaterialRatingDTO(
                        r.getUid(),
                        r.getMaterial().getUid(),
                        r.getRating(),
                        r.getComment(),
                        r.getAuthor(),
                        r.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Double getAverageRating(UUID materialUid) {
        return regRatingRepository.getAverageRatingByMaterialUid(materialUid);
    }

    // ==================== ИНТЕГРАЦИЯ ====================

    public List<MaterialIntegrationDTO> getIntegrations(UUID materialUid) {
        return regIntegrationRepository.findByMaterialUidOrderByCreatedAtDesc(materialUid).stream()
                .map(i -> new MaterialIntegrationDTO(
                        i.getUid(),
                        i.getMaterial().getUid(),
                        i.getEvent(),
                        i.getExchangeType(),
                        i.getDirection(),
                        i.getProtocol(),
                        i.getTargetSystem(),
                        i.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ==================== ЦЕНЫ ====================

    public List<MaterialPriceDTO> getPrices(UUID materialUid) {
        List<RegPrice> prices = priceRepository.findByMaterialUidOrderByPriceDateDesc(materialUid);
        List<MaterialPriceDTO> result = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            RegPrice current = prices.get(i);
            Double previousPrice = (i < prices.size() - 1) ? prices.get(i + 1).getPrice() : null;
            Double priceChange = previousPrice != null ? current.getPrice() - previousPrice : null;
            String customerName = null;
            if (current.getDocEntrance() != null && current.getDocEntrance().getCustomer() != null) {
                customerName = current.getDocEntrance().getCustomer().getName();
            }
            result.add(new MaterialPriceDTO(
                    current.getUid(),
                    current.getPrice(),
                    current.getPriceDate(),
                    customerName,
                    previousPrice,
                    priceChange));
        }
        return result;
    }

    // ==================== ПРИВЯЗКА ЗАКАЗЧИКОВ К МАТЕРИАЛУ ====================

    public List<MaterialSupplyDTO> getMaterialCustomers(UUID materialUid) {
        return regCustomerRepository.findByMaterialUid(materialUid).stream()
                .map(r -> new MaterialSupplyDTO(
                        r.getUid(),
                        r.getMaterial() != null ? r.getMaterial().getUid() : materialUid,
                        null,
                        r.getCustomer() != null ? r.getCustomer().getUid() : null,
                        r.getCustomer() != null ? r.getCustomer().getName() : null,
                        r.getSupplyDate(),
                        r.getDocumentName(),
                        r.getFilePath(),
                        r.getOriginalName(),
                        r.getFilePath() != null ? getFileUrl(materialUid, r.getFilePath()) : null))
                .collect(Collectors.toList());
    }
}