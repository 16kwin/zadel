-- V2__create_zadel_tables.sql
CREATE TABLE zadel_orders_list (
    order_uid VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255),
    order_number VARCHAR(255),
    order_datetime TIMESTAMP WITH TIME ZONE,
    status VARCHAR(255),
    statusreason VARCHAR(255),
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE zadel_orders_full (
    order_uid VARCHAR(255) PRIMARY KEY,
    order_json JSONB NOT NULL,
    FOREIGN KEY (order_uid) REFERENCES zadel_orders_list(order_uid)
);

CREATE TABLE zadel_order_statuses (
    id BIGSERIAL PRIMARY KEY,
    datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    order_uid VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    sub_status VARCHAR(255),
    FOREIGN KEY (order_uid) REFERENCES zadel_orders_list(order_uid)
);

CREATE TABLE zadel_order_tracking (
    id BIGSERIAL PRIMARY KEY,
    datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    order_uid VARCHAR(255) NOT NULL,
    tracking_status VARCHAR(255),
    tracking_sub_status VARCHAR(255),
    FOREIGN KEY (order_uid) REFERENCES zadel_orders_list(order_uid)
);

CREATE TABLE zadel_tkp_list (
    tkp_uid VARCHAR(255) PRIMARY KEY,
    order_uid VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255),
    order_number VARCHAR(255),
    order_datetime TIMESTAMP WITH TIME ZONE,
    total_cost NUMERIC(15, 2),
    delivery_date DATE,
    status VARCHAR(255),
    statusinvoice VARCHAR(255),
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    FOREIGN KEY (order_uid) REFERENCES zadel_orders_list(order_uid)
);

CREATE TABLE zadel_tkp_full (
    tkp_uid VARCHAR(255) PRIMARY KEY,
    order_uid VARCHAR(255) NOT NULL,
    tkp_json JSONB NOT NULL,
    FOREIGN KEY (order_uid) REFERENCES zadel_orders_list(order_uid),
    FOREIGN KEY (tkp_uid) REFERENCES zadel_tkp_list(tkp_uid)
);

CREATE TABLE zadel_tkp_statuses (
    id BIGSERIAL PRIMARY KEY,
    datetime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    tkp_uid VARCHAR(255) NOT NULL,
    order_uid VARCHAR(255),
    status VARCHAR(255),
    sub_status VARCHAR(255),
    FOREIGN KEY (tkp_uid) REFERENCES zadel_tkp_list(tkp_uid),
    FOREIGN KEY (order_uid) REFERENCES zadel_orders_list(order_uid)
);

CREATE INDEX idx_zadel_order_statuses_uid ON zadel_order_statuses(order_uid);
CREATE INDEX idx_zadel_order_tracking_uid ON zadel_order_tracking(order_uid);
CREATE INDEX idx_zadel_tkp_list_order_uid ON zadel_tkp_list(order_uid);
CREATE INDEX idx_zadel_tkp_full_order_uid ON zadel_tkp_full(order_uid);
CREATE INDEX idx_zadel_tkp_statuses_tkp_uid ON zadel_tkp_statuses(tkp_uid);
CREATE INDEX idx_zadel_tkp_statuses_order_uid ON zadel_tkp_statuses(order_uid);