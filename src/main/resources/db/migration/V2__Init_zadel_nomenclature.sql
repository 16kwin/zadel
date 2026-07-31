-- V1__Init_zadel_nomenclature.sql
-- Единая миграция для базы zadel

BEGIN;

-- ============================================================
-- ТИПЫ ДАННЫХ
-- ============================================================

CREATE TABLE IF NOT EXISTS data_type (
    uid UUID PRIMARY KEY,
    type_text TEXT,
    type_number DOUBLE PRECISION,
    type_spr UUID
);

-- ============================================================
-- СПРАВОЧНИКИ НОМЕНКЛАТУРЫ
-- ============================================================

CREATE TABLE IF NOT EXISTS spr_type_material (
    uid UUID PRIMARY KEY,
    type_name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS spr_type_purpose (
    uid UUID PRIMARY KEY,
    type_name TEXT NOT NULL,
    type_material_uid UUID REFERENCES spr_type_material(uid) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS spr_type_product (
    uid UUID PRIMARY KEY,
    type_name TEXT NOT NULL,
    type_purpose_uid UUID REFERENCES spr_type_purpose(uid) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS spr_measure (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS spr_manufacturer (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS spr_brand (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    manufacturer_uid UUID REFERENCES spr_manufacturer(uid) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS spr_model_of_brand (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    brand UUID REFERENCES spr_brand(uid) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS spr_country (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS spr_type_attributes (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    designation VARCHAR(10),
    data_type UUID REFERENCES data_type(uid) ON DELETE SET NULL
);

-- ============================================================
-- ГРУППЫ МАТЕРИАЛОВ
-- ============================================================

CREATE TABLE IF NOT EXISTS reg_group_material (
    uid UUID PRIMARY KEY,
    group_name TEXT NOT NULL,
    parent_group UUID REFERENCES reg_group_material(uid) ON DELETE SET NULL,
    group_code INTEGER
);

-- ============================================================
-- НОМЕНКЛАТУРА
-- ============================================================

CREATE TABLE IF NOT EXISTS reg_attached (
    uid UUID PRIMARY KEY,
    name_file TEXT NOT NULL,
    url_file UUID NOT NULL,
    link UUID
);

CREATE TABLE IF NOT EXISTS reg_attributes (
    uid UUID PRIMARY KEY,
    name UUID REFERENCES spr_type_attributes(uid) ON DELETE SET NULL,
    meaning TEXT,
    measure_uid UUID REFERENCES spr_measure(uid) ON DELETE SET NULL,
    material_uid UUID
);

CREATE TABLE IF NOT EXISTS doc_entrance (
    uid UUID PRIMARY KEY,
    price DOUBLE PRECISION NOT NULL,
    supplier_uid UUID,
    entrance_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_price (
    uid UUID PRIMARY KEY,
    price DOUBLE PRECISION NOT NULL,
    price_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    link UUID,
    doc_entrance_uid UUID REFERENCES doc_entrance(uid) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS reg_customers (
    uid UUID PRIMARY KEY,
    material_uid UUID,
    customer_uid UUID,
    supply_date TIMESTAMP,
    document_name TEXT,
    file_path TEXT,
    original_name TEXT
);

CREATE TABLE IF NOT EXISTS spr_material (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_material SERIAL NOT NULL,
    name_material TEXT,
    article TEXT,
    description TEXT,
    usage BOOLEAN,
    resharpen BOOLEAN,
    waste_material BOOLEAN,
    recycle_material BOOLEAN,
    group_material UUID REFERENCES reg_group_material(uid) ON DELETE SET NULL,
    type_main UUID REFERENCES spr_type_material(uid) ON DELETE SET NULL,
    type_purpose UUID REFERENCES spr_type_purpose(uid) ON DELETE SET NULL,
    type_product UUID REFERENCES spr_type_product(uid) ON DELETE SET NULL,
    manufacturer UUID REFERENCES spr_manufacturer(uid) ON DELETE SET NULL,
    country UUID REFERENCES spr_country(uid) ON DELETE SET NULL,
    brand UUID REFERENCES spr_brand(uid) ON DELETE SET NULL,
    model_of_brand UUID REFERENCES spr_model_of_brand(uid) ON DELETE SET NULL,
    measure UUID REFERENCES spr_measure(uid) ON DELETE SET NULL,
    guid_1c BIT VARYING(128)[],
    uid_other_sys BIT VARYING(128)[],
    uid_store BIT VARYING(128)[],
    url_image UUID,
    attached UUID REFERENCES reg_attached(uid) ON DELETE SET NULL,
    suppliers UUID REFERENCES reg_customers(uid) ON DELETE SET NULL,
    attributes UUID REFERENCES reg_attributes(uid) ON DELETE SET NULL,
    price UUID REFERENCES reg_price(uid) ON DELETE SET NULL,
    syncronized_mother_system BOOLEAN,
    syncronized_supplier BOOLEAN,
    create_date TIME WITHOUT TIME ZONE DEFAULT now()
);

ALTER TABLE reg_attached
    ADD CONSTRAINT reg_attached_link_fkey FOREIGN KEY (link)
    REFERENCES spr_material(uid) ON DELETE SET NULL;

ALTER TABLE reg_attributes
    ADD CONSTRAINT reg_attributes_material_fkey FOREIGN KEY (material_uid)
    REFERENCES spr_material(uid) ON DELETE SET NULL;

ALTER TABLE reg_price
    ADD CONSTRAINT reg_price_link_fkey FOREIGN KEY (link)
    REFERENCES spr_material(uid) ON DELETE SET NULL;

ALTER TABLE reg_customers
    ADD CONSTRAINT reg_customers_material_fkey FOREIGN KEY (material_uid)
    REFERENCES spr_material(uid) ON DELETE SET NULL;

-- ============================================================
-- МЕДИА НОМЕНКЛАТУРЫ
-- ============================================================

CREATE TABLE IF NOT EXISTS spr_material_images (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    original_name TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spr_material_blueprints (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    original_name TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spr_material_codes (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    file_path TEXT,
    original_name TEXT,
    code_type VARCHAR(20) DEFAULT 'QR_CODE',
    code_value TEXT,
    code_kind VARCHAR(20) DEFAULT 'QR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spr_material_documents (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    document_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    original_name TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- АНАЛОГИ, РЕЙТИНГ, ИНТЕГРАЦИЯ, СОБЫТИЯ
-- ============================================================

CREATE TABLE IF NOT EXISTS reg_analog (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    analog_material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    compatibility_percent INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_rating (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 0 AND rating <= 5),
    comment TEXT,
    author TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_integration (
    uid UUID PRIMARY KEY,
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    event TEXT NOT NULL DEFAULT 'Объект синхронизирован',
    exchange_type TEXT NOT NULL,
    direction TEXT NOT NULL,
    protocol TEXT NOT NULL,
    target_system TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_event_log (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_uid UUID NOT NULL REFERENCES spr_material(uid) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT NOT NULL,
    field_name VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    author VARCHAR(255),
    source VARCHAR(100) DEFAULT 'Через карточку',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ЗАКАЗЧИКИ
-- ============================================================

CREATE TABLE IF NOT EXISTS spr_customer_description_types (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS spr_customers (
    uid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    code INTEGER,
    country_uid UUID REFERENCES spr_country(uid) ON DELETE SET NULL,
    address TEXT,
    short_description_uid UUID REFERENCES spr_customer_description_types(uid) ON DELETE SET NULL,
    description TEXT,
    email TEXT,
    website TEXT,
    phone TEXT,
    brand_uid UUID REFERENCES spr_brand(uid) ON DELETE SET NULL,
    inn TEXT,
    ogrn TEXT,
    kpp TEXT,
    contact_person TEXT,
    contact_position TEXT,
    contact_phone TEXT,
    director TEXT,
    director_position TEXT,
    bank_name TEXT,
    bik TEXT,
    correspondent_account TEXT,
    settlement_account TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spr_customer_images (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_uid UUID NOT NULL REFERENCES spr_customers(uid) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    original_name TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spr_customer_documents (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_uid UUID NOT NULL REFERENCES spr_customers(uid) ON DELETE CASCADE,
    document_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    original_name TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_customer_ratings (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_uid UUID NOT NULL REFERENCES spr_customers(uid) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    author TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_customer_integration (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_uid UUID NOT NULL REFERENCES spr_customers(uid) ON DELETE CASCADE,
    event TEXT,
    exchange_type TEXT,
    direction TEXT,
    protocol TEXT,
    target_system TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reg_customer_event_log (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_uid UUID NOT NULL REFERENCES spr_customers(uid) ON DELETE CASCADE,
    event_type TEXT,
    event_description TEXT,
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    author TEXT,
    source TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FK для doc_entrance и reg_customers
ALTER TABLE doc_entrance
    ADD CONSTRAINT doc_entrance_supplier_fkey FOREIGN KEY (supplier_uid)
    REFERENCES spr_customers(uid) ON DELETE SET NULL;

ALTER TABLE reg_customers
    ADD CONSTRAINT reg_customers_customer_fkey FOREIGN KEY (customer_uid)
    REFERENCES spr_customers(uid) ON DELETE SET NULL;

-- ============================================================
-- ИНДЕКСЫ
-- ============================================================

CREATE INDEX idx_spr_material_code ON spr_material(code_material);
CREATE INDEX idx_spr_material_group ON spr_material(group_material);
CREATE INDEX idx_spr_material_article ON spr_material(article);
CREATE INDEX idx_reg_attributes_material ON reg_attributes(material_uid);
CREATE INDEX idx_reg_event_log_material ON reg_event_log(material_uid);
CREATE INDEX idx_reg_event_log_created ON reg_event_log(created_at DESC);
CREATE INDEX idx_spr_customers_code ON spr_customers(code);
CREATE INDEX idx_spr_customers_country ON spr_customers(country_uid);
CREATE INDEX idx_spr_customer_images_customer ON spr_customer_images(customer_uid);
CREATE INDEX idx_spr_customer_documents_customer ON spr_customer_documents(customer_uid);
CREATE INDEX idx_reg_customer_ratings_customer ON reg_customer_ratings(customer_uid);
CREATE INDEX idx_reg_customer_integration_customer ON reg_customer_integration(customer_uid);
CREATE INDEX idx_reg_customer_event_log_customer ON reg_customer_event_log(customer_uid);

-- ============================================================
-- НАЧАЛЬНЫЕ ДАННЫЕ
-- ============================================================

-- Единицы измерения
INSERT INTO spr_measure (uid, name, description) VALUES
    (gen_random_uuid(), 'мм', 'Миллиметр'),
    (gen_random_uuid(), 'см', 'Сантиметр'),
    (gen_random_uuid(), 'м', 'Метр'),
    (gen_random_uuid(), 'шт', 'Штука'),
    (gen_random_uuid(), 'кг', 'Килограмм'),
    (gen_random_uuid(), 'л', 'Литр'),
    (gen_random_uuid(), 'град', 'Градус'),
    (gen_random_uuid(), 'компл', 'Комплект')
ON CONFLICT DO NOTHING;

-- Страны
INSERT INTO spr_country (uid, name) VALUES
    (gen_random_uuid(), 'Россия'),
    (gen_random_uuid(), 'Германия'),
    (gen_random_uuid(), 'Япония'),
    (gen_random_uuid(), 'США'),
    (gen_random_uuid(), 'Китай'),
    (gen_random_uuid(), 'Италия'),
    (gen_random_uuid(), 'Франция'),
    (gen_random_uuid(), 'Беларусь')
ON CONFLICT DO NOTHING;

-- Виды характеристик
INSERT INTO spr_type_attributes (uid, name, designation) VALUES
    (gen_random_uuid(), 'Длина', 'L'),
    (gen_random_uuid(), 'Ширина', 'W'),
    (gen_random_uuid(), 'Глубина', 'D'),
    (gen_random_uuid(), 'Высота', 'H'),
    (gen_random_uuid(), 'Масса', 'M'),
    (gen_random_uuid(), 'Срок эксплуатации', 'T'),
    (gen_random_uuid(), 'Стандарт исполнения', 'ГОСТ/DIN'),
    (gen_random_uuid(), 'Покрытие', 'Покр.'),
    (gen_random_uuid(), 'Тип хвостовика', 'Хвост.'),
    (gen_random_uuid(), 'Глубина сверления', 'Глуб.'),
    (gen_random_uuid(), 'Угол заточки', 'Уг.зат.'),
    (gen_random_uuid(), 'Тип охлаждения', 'Охл.'),
    (gen_random_uuid(), 'Материал инструмента', 'Матер.'),
    (gen_random_uuid(), 'Назначение', 'Назн.'),
    (gen_random_uuid(), 'Группа обрабатываемых материалов', 'Гр.обр.'),
    (gen_random_uuid(), 'Особенность инструмента', 'Особ.')
ON CONFLICT DO NOTHING;

-- Группы учета
INSERT INTO spr_type_material (uid, type_name) VALUES
    (gen_random_uuid(), 'ТМЦ'),
    (gen_random_uuid(), 'Готовая деталь')
ON CONFLICT DO NOTHING;

-- Группы номенклатуры
DO $$
DECLARE
    tmc_uid UUID;
    ready_uid UUID;
BEGIN
    SELECT uid INTO tmc_uid FROM spr_type_material WHERE type_name = 'ТМЦ';
    SELECT uid INTO ready_uid FROM spr_type_material WHERE type_name = 'Готовая деталь';

    INSERT INTO spr_type_purpose (uid, type_name, type_material_uid) VALUES
        (gen_random_uuid(), 'Металлообрабатывающий инструмент', tmc_uid),
        (gen_random_uuid(), 'Слесарный инструмент', tmc_uid),
        (gen_random_uuid(), 'Оснастка', tmc_uid),
        (gen_random_uuid(), 'Готовые детали', ready_uid)
    ON CONFLICT DO NOTHING;
END $$;

-- Виды номенклатуры
DO $$
DECLARE
    metal_uid UUID;
    slesar_uid UUID;
    osnastka_uid UUID;
    ready_uid UUID;
BEGIN
    SELECT uid INTO metal_uid FROM spr_type_purpose WHERE type_name = 'Металлообрабатывающий инструмент';
    SELECT uid INTO slesar_uid FROM spr_type_purpose WHERE type_name = 'Слесарный инструмент';
    SELECT uid INTO osnastka_uid FROM spr_type_purpose WHERE type_name = 'Оснастка';
    SELECT uid INTO ready_uid FROM spr_type_purpose WHERE type_name = 'Готовые детали';

    INSERT INTO spr_type_product (uid, type_name, type_purpose_uid) VALUES
        (gen_random_uuid(), 'Сверло', metal_uid),
        (gen_random_uuid(), 'Фреза', metal_uid),
        (gen_random_uuid(), 'Резец', metal_uid),
        (gen_random_uuid(), 'Метчик', metal_uid),
        (gen_random_uuid(), 'Молоток', slesar_uid),
        (gen_random_uuid(), 'Отвертка', slesar_uid),
        (gen_random_uuid(), 'Ключ гаечный', slesar_uid),
        (gen_random_uuid(), 'Тиски', osnastka_uid),
        (gen_random_uuid(), 'Патрон', osnastka_uid),
        (gen_random_uuid(), 'Кондуктор', osnastka_uid),
        (gen_random_uuid(), 'Вал', ready_uid),
        (gen_random_uuid(), 'Втулка', ready_uid),
        (gen_random_uuid(), 'Корпус', ready_uid),
        (gen_random_uuid(), 'Крышка', ready_uid)
    ON CONFLICT DO NOTHING;
END $$;

-- Корневая группа номенклатуры
INSERT INTO reg_group_material (uid, group_name, parent_group, group_code)
SELECT gen_random_uuid(), 'Номенклатура', NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM reg_group_material WHERE group_name = 'Номенклатура');

-- Типы описаний заказчиков
INSERT INTO spr_customer_description_types (name) VALUES
    ('Промышленное предприятие'),
    ('Торговая компания'),
    ('Строительная организация'),
    ('Научно-исследовательский институт'),
    ('Сервисная компания')
ON CONFLICT DO NOTHING;

-- Производитель New Century
INSERT INTO spr_manufacturer (uid, name, description)
SELECT gen_random_uuid(), 'New Century', 'New Century Drill'
WHERE NOT EXISTS (SELECT 1 FROM spr_manufacturer WHERE name = 'New Century');

-- Бренд New Century
DO $$
DECLARE
    v_manufacturer_uid UUID;
BEGIN
    SELECT uid INTO v_manufacturer_uid FROM spr_manufacturer WHERE name = 'New Century';
    
    INSERT INTO spr_brand (uid, name, description, manufacturer_uid)
    SELECT gen_random_uuid(), 'New Century', 'New Century Drill', v_manufacturer_uid
    WHERE NOT EXISTS (SELECT 1 FROM spr_brand WHERE name = 'New Century');
END $$;

-- Модель DH224
DO $$
DECLARE
    v_brand_uid UUID;
BEGIN
    SELECT uid INTO v_brand_uid FROM spr_brand WHERE name = 'New Century';
    
    INSERT INTO spr_model_of_brand (uid, name, description, brand)
    SELECT gen_random_uuid(), 'DH224', 'Серия DH224 5XD', v_brand_uid
    WHERE NOT EXISTS (SELECT 1 FROM spr_model_of_brand WHERE name = 'DH224');
END $$;

-- Группа материалов "Сверла твердосплавные"
DO $$
DECLARE
    v_root_uid UUID;
BEGIN
    SELECT uid INTO v_root_uid FROM reg_group_material WHERE parent_group IS NULL LIMIT 1;
    
    INSERT INTO reg_group_material (uid, group_name, parent_group, group_code)
    SELECT gen_random_uuid(), 'Сверла твердосплавные', v_root_uid, 1
    WHERE NOT EXISTS (SELECT 1 FROM reg_group_material WHERE group_name = 'Сверла твердосплавные');
END $$;

-- ============================================================
-- ЗАГРУЗКА 10 СВЁРЛ
-- ============================================================

DO $$
DECLARE
    v_tmc_uid UUID;
    v_purpose_uid UUID;
    v_product_uid UUID;
    v_measure_mm_uid UUID;
    v_measure_deg_uid UUID;
    v_manufacturer_uid UUID;
    v_brand_uid UUID;
    v_model_uid UUID;
    v_country_uid UUID;
    v_group_uid UUID;
    
    v_attr_length_uid UUID;
    v_attr_width_uid UUID;
    v_attr_height_uid UUID;
    v_attr_mass_uid UUID;
    v_attr_standard_uid UUID;
    v_attr_coating_uid UUID;
    v_attr_shank_uid UUID;
    v_attr_depth_uid UUID;
    v_attr_angle_uid UUID;
    v_attr_cooling_uid UUID;
    v_attr_material_uid UUID;
    v_attr_purpose_uid UUID;
    v_attr_material_group_uid UUID;
    v_attr_feature_uid UUID;
    
    v_material_uid UUID;
    v_code INTEGER;
    v_photo_uid UUID;
    v_blueprint_uid UUID;
    
    v_data TEXT[][] := ARRAY[
        ARRAY['1', '8', 'DH2240100', 'Сверло твердосплавное 5XD с покрытием TiАIN 1X3X8X55'],
        ARRAY['1.1', '12', 'DH2240110', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.1X3X12X55'],
        ARRAY['1.2', '12', 'DH2240120', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.2X3X12X55'],
        ARRAY['1.3', '12', 'DH2240130', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.3X3X12X55'],
        ARRAY['1.4', '12', 'DH2240140', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.4X3X12X55'],
        ARRAY['1.5', '16', 'DH2240150', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.5X3X16X55'],
        ARRAY['1.6', '16', 'DH2240160', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.6X3X16X55'],
        ARRAY['1.7', '16', 'DH2240170', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.7X3X16X55'],
        ARRAY['1.8', '16', 'DH2240180', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.8X3X16X55'],
        ARRAY['1.83', '16', 'DH2240183', 'Сверло твердосплавное 5XD с покрытием TiАIN 1.83X3X16X55']
    ];
    
    v_description TEXT := 'Спиральное сверло из твердого сплава с правым вращением и цилиндрическим хвостовиком без каналов для подачи охлаждающей жидкости. Идеально подходит для сверления стали общего назначения, легированных сталей, чугуна и закаленных материалов до HRc 55. Не требует предварительной зацентровки, так как самоцентрируется. Специальная конструкция исключает необходимость развертывания отверстий. Эффективный отвод стружки повышает производительность. Покрытие из нитрида титана-алюминия (TiAlN) увеличивает износостойкость и улучшает рабочие характеристики инструмента.';
    
    v_idx INTEGER;
    v_diameter TEXT;
    v_flute_length TEXT;
    v_article TEXT;
    v_name TEXT;
BEGIN
    SELECT uid INTO v_tmc_uid FROM spr_type_material WHERE type_name = 'ТМЦ';
    SELECT uid INTO v_purpose_uid FROM spr_type_purpose WHERE type_name = 'Металлообрабатывающий инструмент';
    SELECT uid INTO v_product_uid FROM spr_type_product WHERE type_name = 'Сверло';
    SELECT uid INTO v_measure_mm_uid FROM spr_measure WHERE name = 'мм';
    SELECT uid INTO v_measure_deg_uid FROM spr_measure WHERE name = 'град';
    SELECT uid INTO v_manufacturer_uid FROM spr_manufacturer WHERE name = 'New Century';
    SELECT uid INTO v_brand_uid FROM spr_brand WHERE name = 'New Century';
    SELECT uid INTO v_model_uid FROM spr_model_of_brand WHERE name = 'DH224';
    SELECT uid INTO v_country_uid FROM spr_country WHERE name = 'Китай';
    SELECT uid INTO v_group_uid FROM reg_group_material WHERE group_name = 'Сверла твердосплавные';
    
    SELECT uid INTO v_attr_length_uid FROM spr_type_attributes WHERE name = 'Длина';
    SELECT uid INTO v_attr_width_uid FROM spr_type_attributes WHERE name = 'Ширина';
    SELECT uid INTO v_attr_height_uid FROM spr_type_attributes WHERE name = 'Высота';
    SELECT uid INTO v_attr_mass_uid FROM spr_type_attributes WHERE name = 'Масса';
    SELECT uid INTO v_attr_standard_uid FROM spr_type_attributes WHERE name = 'Стандарт исполнения';
    SELECT uid INTO v_attr_coating_uid FROM spr_type_attributes WHERE name = 'Покрытие';
    SELECT uid INTO v_attr_shank_uid FROM spr_type_attributes WHERE name = 'Тип хвостовика';
    SELECT uid INTO v_attr_depth_uid FROM spr_type_attributes WHERE name = 'Глубина сверления';
    SELECT uid INTO v_attr_angle_uid FROM spr_type_attributes WHERE name = 'Угол заточки';
    SELECT uid INTO v_attr_cooling_uid FROM spr_type_attributes WHERE name = 'Тип охлаждения';
    SELECT uid INTO v_attr_material_uid FROM spr_type_attributes WHERE name = 'Материал инструмента';
    SELECT uid INTO v_attr_purpose_uid FROM spr_type_attributes WHERE name = 'Назначение';
    SELECT uid INTO v_attr_material_group_uid FROM spr_type_attributes WHERE name = 'Группа обрабатываемых материалов';
    SELECT uid INTO v_attr_feature_uid FROM spr_type_attributes WHERE name = 'Особенность инструмента';
    
    SELECT COALESCE(MAX(code_material), 0) INTO v_code FROM spr_material;
    
    FOR v_idx IN 1..10 LOOP
        v_diameter := v_data[v_idx][1];
        v_flute_length := v_data[v_idx][2];
        v_article := v_data[v_idx][3];
        v_name := v_data[v_idx][4];
        
        v_code := v_code + 1;
        v_material_uid := gen_random_uuid();
        
        INSERT INTO spr_material (
            uid, code_material, name_material, article, description,
            group_material, type_main, type_purpose, type_product,
            manufacturer, brand, model_of_brand, country, measure,
            usage, resharpen, waste_material, recycle_material
        ) VALUES (
            v_material_uid, v_code, v_name, v_article, v_description,
            v_group_uid, v_tmc_uid, v_purpose_uid, v_product_uid,
            v_manufacturer_uid, v_brand_uid, v_model_uid, v_country_uid, v_measure_mm_uid,
            true, false, false, false
        );
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_length_uid, '55', v_measure_mm_uid, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_width_uid, v_diameter, v_measure_mm_uid, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_height_uid, '3', v_measure_mm_uid, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_mass_uid, '0', v_measure_mm_uid, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_standard_uid, 'DIN6539', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_coating_uid, 'TiAlN', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_shank_uid, 'HA-Цилиндр', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_depth_uid, '5xD', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_angle_uid, '140', v_measure_deg_uid, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_cooling_uid, 'Внешнее', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_material_uid, 'HM-Твердый сплав', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_purpose_uid, 'Универсальные', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_material_group_uid, 'P-стали; M-нержавеющие стали; K-чугун; H-Твердые закаленные материалы', NULL, v_material_uid);
        
        INSERT INTO reg_attributes (uid, name, meaning, measure_uid, material_uid)
        VALUES (gen_random_uuid(), v_attr_feature_uid, 'Удлиненное (от 5 до 10хD)', NULL, v_material_uid);
        
        -- Фото
        v_photo_uid := gen_random_uuid();
        INSERT INTO spr_material_images (uid, material_uid, file_path, original_name, sort_order, created_at)
        VALUES (v_photo_uid, v_material_uid, v_photo_uid::text || '.png', v_article || '_photo.png', 0, NOW());
        
        -- Чертеж
        v_blueprint_uid := gen_random_uuid();
        INSERT INTO spr_material_blueprints (uid, material_uid, file_path, original_name, created_at)
        VALUES (v_blueprint_uid, v_material_uid, v_blueprint_uid::text || '.png', v_article || '_blueprint.png', NOW());
        
    END LOOP;
    
END $$;

-- ============================================================
-- ЗАГРУЗКА 5 ЗАКАЗЧИКОВ
-- ============================================================

DO $$
DECLARE
    v_country_rus UUID;
    v_country_blr UUID;
    v_country_chn UUID;
    
    v_desc_industrial UUID;
    v_desc_trade UUID;
    v_desc_construction UUID;
    v_desc_science UUID;
    v_desc_service UUID;
    
    v_code INTEGER;
    v_customer_uid UUID;
    v_logo_uid UUID;
    v_logo_filename TEXT;
BEGIN
    SELECT uid INTO v_country_rus FROM spr_country WHERE name = 'Россия';
    SELECT uid INTO v_country_blr FROM spr_country WHERE name = 'Беларусь';
    SELECT uid INTO v_country_chn FROM spr_country WHERE name = 'Китай';
    
    SELECT uid INTO v_desc_industrial FROM spr_customer_description_types WHERE name = 'Промышленное предприятие';
    SELECT uid INTO v_desc_trade FROM spr_customer_description_types WHERE name = 'Торговая компания';
    SELECT uid INTO v_desc_construction FROM spr_customer_description_types WHERE name = 'Строительная организация';
    SELECT uid INTO v_desc_science FROM spr_customer_description_types WHERE name = 'Научно-исследовательский институт';
    SELECT uid INTO v_desc_service FROM spr_customer_description_types WHERE name = 'Сервисная компания';
    
    SELECT COALESCE(MAX(code), 0) INTO v_code FROM spr_customers;
    
    -- Заказчик 1: ООО "ПромТехМаш"
    v_code := v_code + 1;
    v_customer_uid := gen_random_uuid();
    INSERT INTO spr_customers (
        uid, code, name, country_uid, address, short_description_uid, description,
        email, website, phone,
        inn, ogrn, kpp,
        contact_person, contact_position, contact_phone,
        director, director_position,
        bank_name, bik, correspondent_account, settlement_account
    ) VALUES (
        v_customer_uid, v_code, 'ООО "ПромТехМаш"',
        v_country_rus, '125212, г. Москва, ул. Адмирала Макарова, д. 10, стр. 1, офис 45',
        v_desc_industrial,
        'Крупное машиностроительное предприятие. Специализация: производство металлообрабатывающих станков, оснастки, комплектующих. Более 20 лет на рынке.',
        'info@promtexmash.ru', 'www.promtexmash.ru', '+7 (495) 123-45-67',
        '7712345678', '1027700123456', '771201001',
        'Петров Сергей Владимирович', 'Руководитель отдела закупок', '+7 (495) 123-45-68',
        'Кузнецов Алексей Николаевич', 'Генеральный директор',
        'ПАО "Сбербанк"', '044525225', '30101810400000000225', '40702810900000000123'
    );
    v_logo_uid := gen_random_uuid();
    v_logo_filename := v_logo_uid::text || '.svg';
    INSERT INTO spr_customer_images (uid, customer_uid, file_path, original_name, sort_order, created_at)
    VALUES (v_logo_uid, v_customer_uid, v_logo_filename, 'PromTexMash_logo.svg', 0, NOW());
    
    -- Заказчик 2: АО "ТехноСтрой"
    v_code := v_code + 1;
    v_customer_uid := gen_random_uuid();
    INSERT INTO spr_customers (
        uid, code, name, country_uid, address, short_description_uid, description,
        email, website, phone,
        inn, ogrn, kpp,
        contact_person, contact_position, contact_phone,
        director, director_position,
        bank_name, bik, correspondent_account, settlement_account
    ) VALUES (
        v_customer_uid, v_code, 'АО "ТехноСтрой"',
        v_country_rus, '620014, г. Екатеринбург, ул. Малышева, д. 51, офис 302',
        v_desc_construction,
        'Ведущая строительная организация Уральского региона. Выполняет полный цикл строительно-монтажных работ. Собственное производство металлоконструкций.',
        'sales@technostroy.ru', 'www.technostroy.ru', '+7 (343) 234-56-78',
        '6671234567', '1036600123456', '667101001',
        'Смирнова Елена Александровна', 'Начальник отдела снабжения', '+7 (343) 234-56-79',
        'Морозов Дмитрий Игоревич', 'Генеральный директор',
        'АО "Альфа-Банк"', '044525593', '30101810200000000593', '40702810300000000456'
    );
    v_logo_uid := gen_random_uuid();
    v_logo_filename := v_logo_uid::text || '.svg';
    INSERT INTO spr_customer_images (uid, customer_uid, file_path, original_name, sort_order, created_at)
    VALUES (v_logo_uid, v_customer_uid, v_logo_filename, 'TechnoStroy_logo.svg', 0, NOW());
    
    -- Заказчик 3: ООО "БелСтройТорг"
    v_code := v_code + 1;
    v_customer_uid := gen_random_uuid();
    INSERT INTO spr_customers (
        uid, code, name, country_uid, address, short_description_uid, description,
        email, website, phone,
        inn, ogrn, kpp,
        contact_person, contact_position, contact_phone,
        director, director_position,
        bank_name, bik, correspondent_account, settlement_account
    ) VALUES (
        v_customer_uid, v_code, 'ООО "БелСтройТорг"',
        v_country_blr, '220030, Республика Беларусь, г. Минск, ул. Интернациональная, д. 15',
        v_desc_trade,
        'Торговая компания, специализирующаяся на поставках строительных материалов и инструмента. Работаем с 2010 года. Широкая дилерская сеть по всей Беларуси.',
        'info@belstroytorg.by', 'www.belstroytorg.by', '+375 (17) 345-67-89',
        '192345678', '304192345600012', '—',
        'Иванов Александр Александрович', 'Руководитель отдела закупок', '+375 (29) 111-22-33',
        'Иванов Александр Александрович', 'Директор',
        'ОАО "АСБ Беларусбанк"', '153001795', '30101810200000000795', '40702810900000000789'
    );
    v_logo_uid := gen_random_uuid();
    v_logo_filename := v_logo_uid::text || '.svg';
    INSERT INTO spr_customer_images (uid, customer_uid, file_path, original_name, sort_order, created_at)
    VALUES (v_logo_uid, v_customer_uid, v_logo_filename, 'BelStroyTorg_logo.svg', 0, NOW());
    
    -- Заказчик 4: НИИ "ТочМаш"
    v_code := v_code + 1;
    v_customer_uid := gen_random_uuid();
    INSERT INTO spr_customers (
        uid, code, name, country_uid, address, short_description_uid, description,
        email, website, phone,
        inn, ogrn, kpp,
        contact_person, contact_position, contact_phone,
        director, director_position,
        bank_name, bik, correspondent_account, settlement_account
    ) VALUES (
        v_customer_uid, v_code, 'НИИ "ТочМаш"',
        v_country_chn, '430000, Китай, г. Шанхай, Pudong New Area, Zhangjiang Hi-Tech Park, Building 12',
        v_desc_science,
        'Научно-исследовательский институт точного машиностроения. Разработка и внедрение инновационных технологий в области металлообработки. Международное сотрудничество.',
        'order@tochmash.pro', 'www.tochmash.pro', '+86 (21) 1234-5678',
        '9901234567', '1039900123456', '990101001',
        'Чжан Вэй', 'Руководитель отдела ВЭД', '+86 (21) 1234-5679',
        'Ли Цзянь', 'Директор института',
        'Bank of China, Shanghai Branch', 'BKCHCNBJ300', '30101810200000000300', '40702810900000001314'
    );
    v_logo_uid := gen_random_uuid();
    v_logo_filename := v_logo_uid::text || '.svg';
    INSERT INTO spr_customer_images (uid, customer_uid, file_path, original_name, sort_order, created_at)
    VALUES (v_logo_uid, v_customer_uid, v_logo_filename, 'TochMash_logo.svg', 0, NOW());
    
    -- Заказчик 5: ООО "СервисТех"
    v_code := v_code + 1;
    v_customer_uid := gen_random_uuid();
    INSERT INTO spr_customers (
        uid, code, name, country_uid, address, short_description_uid, description,
        email, website, phone,
        inn, ogrn, kpp,
        contact_person, contact_position, contact_phone,
        director, director_position,
        bank_name, bik, correspondent_account, settlement_account
    ) VALUES (
        v_customer_uid, v_code, 'ООО "СервисТех"',
        v_country_rus, '105264, г. Москва, ул. Верхняя Первомайская, д. 47, стр. 3',
        v_desc_service,
        'Сервисная компания по обслуживанию и ремонту промышленного оборудования. Оказывает услуги по модернизации станков, пуско-наладке, техническому аудиту.',
        'info@servicetech.pro', 'www.servicetech.pro', '+7 (495) 987-65-43',
        '7719876543', '1027700987654', '771901001',
        'Григорьев Андрей Павлович', 'Начальник отдела снабжения', '+7 (495) 987-65-44',
        'Соколов Михаил Леонидович', 'Генеральный директор',
        'ПАО "Сбербанк"', '044525225', '30101810400000000225', '40702810900000005678'
    );
    v_logo_uid := gen_random_uuid();
    v_logo_filename := v_logo_uid::text || '.svg';
    INSERT INTO spr_customer_images (uid, customer_uid, file_path, original_name, sort_order, created_at)
    VALUES (v_logo_uid, v_customer_uid, v_logo_filename, 'ServiceTech_logo.svg', 0, NOW());
    
END $$;

COMMIT;