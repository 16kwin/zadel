package com.example.zadel.config;

import com.example.zadel.model.SprMaterialBlueprint;
import com.example.zadel.model.SprMaterialImage;
import com.example.zadel.model.SprCustomerImage;
import com.example.zadel.repository.SprMaterialBlueprintRepository;
import com.example.zadel.repository.SprMaterialImageRepository;
import com.example.zadel.repository.SprCustomerImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationPhotoImporter implements ApplicationRunner {

    private final SprMaterialImageRepository imageRepository;
    private final SprMaterialBlueprintRepository blueprintRepository;
    private final SprCustomerImageRepository customerImageRepository;

    private static final String NOMENCLATURE_UPLOAD_DIR = "uploads/nomenclature/";
    private static final String CUSTOMER_UPLOAD_DIR = "uploads/customers/";
    private static final String MIGRATION_PHOTOS_DIR = "migration_photos/";
    private static final String FLAG_FILE = "uploads/nomenclature/.migration_photos_imported";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path flagPath = Path.of(FLAG_FILE);
        if (Files.exists(flagPath)) {
            log.info("Migration photos already imported, skipping.");
            return;
        }

        log.info("Starting migration photos import...");

        Path sourceDir = Path.of(MIGRATION_PHOTOS_DIR);
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            log.warn("Migration photos directory not found: {}. Skipping photo import.", sourceDir.toAbsolutePath());
            return;
        }

        int importedImages = 0;
        int importedBlueprints = 0;
        int importedCustomerLogos = 0;
        int errors = 0;

        // Импорт фото номенклатуры
        List<SprMaterialImage> images = imageRepository.findAll();
        for (SprMaterialImage image : images) {
            try {
                Path materialDir = Path.of(NOMENCLATURE_UPLOAD_DIR, image.getMaterial().getUid().toString());
                if (!Files.exists(materialDir)) Files.createDirectories(materialDir);
                Path sourceFile = sourceDir.resolve(image.getOriginalName());
                if (Files.exists(sourceFile)) {
                    Path targetFile = materialDir.resolve(image.getFilePath());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    importedImages++;
                    log.debug("Imported image: {} -> {}", image.getOriginalName(), targetFile);
                } else {
                    log.warn("Source image not found: {}", sourceFile.toAbsolutePath());
                    errors++;
                }
            } catch (IOException e) {
                log.error("Error importing image: {}", e.getMessage());
                errors++;
            }
        }

        // Импорт чертежей номенклатуры
        List<SprMaterialBlueprint> blueprints = blueprintRepository.findAll();
        for (SprMaterialBlueprint blueprint : blueprints) {
            try {
                Path materialDir = Path.of(NOMENCLATURE_UPLOAD_DIR, blueprint.getMaterial().getUid().toString());
                if (!Files.exists(materialDir)) Files.createDirectories(materialDir);
                Path sourceFile = sourceDir.resolve(blueprint.getOriginalName());
                if (Files.exists(sourceFile)) {
                    Path targetFile = materialDir.resolve(blueprint.getFilePath());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    importedBlueprints++;
                    log.debug("Imported blueprint: {} -> {}", blueprint.getOriginalName(), targetFile);
                } else {
                    log.warn("Source blueprint not found: {}", sourceFile.toAbsolutePath());
                    errors++;
                }
            } catch (IOException e) {
                log.error("Error importing blueprint: {}", e.getMessage());
                errors++;
            }
        }

        // Импорт логотипов заказчиков
        List<SprCustomerImage> customerImages = customerImageRepository.findAll();
        for (SprCustomerImage img : customerImages) {
            try {
                Path customerDir = Path.of(CUSTOMER_UPLOAD_DIR, img.getCustomer().getUid().toString());
                if (!Files.exists(customerDir)) Files.createDirectories(customerDir);
                Path sourceFile = sourceDir.resolve(img.getOriginalName());
                if (Files.exists(sourceFile)) {
                    Path targetFile = customerDir.resolve(img.getFilePath());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    importedCustomerLogos++;
                    log.debug("Imported customer logo: {} -> {}", img.getOriginalName(), targetFile);
                } else {
                    log.warn("Source customer logo not found: {}", sourceFile.toAbsolutePath());
                    errors++;
                }
            } catch (IOException e) {
                log.error("Error importing customer logo: {}", e.getMessage());
                errors++;
            }
        }

        if (errors == 0) {
            try {
                Files.createDirectories(Path.of(NOMENCLATURE_UPLOAD_DIR));
                Files.createFile(flagPath);
                log.info("Migration photos import completed successfully. Images: {}, Blueprints: {}, Customer logos: {}",
                    importedImages, importedBlueprints, importedCustomerLogos);
            } catch (IOException e) {
                log.error("Error creating flag file: {}", e.getMessage());
            }
        } else {
            log.warn("Migration photos import completed with {} errors. Images: {}, Blueprints: {}, Customer logos: {}. Fix errors and delete flag file to retry.",
                errors, importedImages, importedBlueprints, importedCustomerLogos);
        }
    }
}