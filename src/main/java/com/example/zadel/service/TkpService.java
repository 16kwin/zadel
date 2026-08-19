// ЗАДЕЛ — service/TkpService.java — ПОЛНЫЙ ФАЙЛ С ЛОГАМИ + order_number
package com.example.zadel.service;

import com.example.zadel.dto.*;
import com.example.zadel.model.*;
import com.example.zadel.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TkpService {

    private final TkpListRepository tkpListRepository;
    private final TkpFullRepository tkpFullRepository;
    private final TkpStatusRepository tkpStatusRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OrdersListRepository ordersListRepository;
    private final OrderService orderService;
    private final NomenclatureService nomenclatureService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${saas.service.url}")
    private String saasServiceUrl;

    @Value("${api.key.zadel}")
    private String apiKey;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    public List<Map<String, Object>> getOutgoingTkp() {
        log.info("Zadel TkpService: getOutgoingTkp() вызван");
        return tkpListRepository.findByStatus("active")
                .stream().map(this::toTkpListMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getClosedTkp() {
        log.info("Zadel TkpService: getClosedTkp() вызван");
        return tkpListRepository.findByStatus("closed")
                .stream().map(this::toTkpListMap).collect(Collectors.toList());
    }

    public Optional<Map<String, Object>> getTkp(String tkpUid) {
        log.info("Zadel TkpService: getTkp() вызван, tkpUid={}", tkpUid);
        Optional<TkpFull> fullOpt = tkpFullRepository.findByTkpUid(tkpUid);
        if (fullOpt.isPresent()) {
            try {
                Map<String, Object> tkp = objectMapper.readValue(fullOpt.get().getTkpJson(), Map.class);
                tkp.put("status", getLatestTkpStatus(tkpUid));
                tkp.put("statusinvoice", getLatestStatusInvoice(tkpUid));
                
                // Добавляем номер заказа
                String orderUid = (String) tkp.get("order_uid");
                if (orderUid != null) {
                    ordersListRepository.findById(orderUid).ifPresent(order -> {
                        tkp.put("ordernumber", order.getOrderNumber());
                    });
                }
                
                return Optional.of(tkp);
            } catch (Exception e) {
                log.error("Zadel TkpService: ошибка парсинга ТКП {}", tkpUid, e);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void saveTkpLocally(String tkpUid, Map<String, Object> tkpData) {
        log.info("Zadel TkpService: saveTkpLocally() вызван, tkpUid={}, statusinvoice={}", tkpUid, tkpData.get("statusinvoice"));
        TkpList tkpList = TkpList.builder()
                .tkpUid(tkpUid)
                .orderUid((String) tkpData.get("order_uid"))
                .customerId((String) tkpData.get("customer"))
                .orderNumber((String) tkpData.get("tkp_number"))
                .orderDatetime(parseDateTime((String) tkpData.get("tkp_data")))
                .totalCost(tkpData.get("total_cost") != null ? new BigDecimal(tkpData.get("total_cost").toString()) : null)
                .deliveryDate(tkpData.get("delivery_date") != null ? LocalDate.parse(tkpData.get("delivery_date").toString()) : null)
                .status((String) tkpData.getOrDefault("status", "active"))
                .statusinvoice((String) tkpData.get("statusinvoice"))
                .syncedAt(ZonedDateTime.now())
                .build();
        tkpListRepository.save(tkpList);

        try {
            String json = objectMapper.writeValueAsString(tkpData);
            TkpFull tkpFull = TkpFull.builder()
                    .tkpUid(tkpUid)
                    .orderUid((String) tkpData.get("order_uid"))
                    .tkpJson(json)
                    .build();
            tkpFullRepository.save(tkpFull);
        } catch (Exception e) {
            log.error("Zadel TkpService: ошибка сохранения TkpFull {}", tkpUid, e);
        }
    }

    @Transactional
    public void receiveTkp(String tkpUid, Map<String, Object> request) {
        log.info("Zadel TkpService: receiveTkp() вызван, tkpUid={}", tkpUid);
        saveTkpLocally(tkpUid, request);

        TkpStatus tkpStatus = TkpStatus.builder()
                .tkpUid(tkpUid)
                .orderUid((String) request.get("order_uid"))
                .datetime(ZonedDateTime.now())
                .build();
        tkpStatusRepository.save(tkpStatus);
    }

    @Transactional
    public void receiveTkpStatusUpdate(String tkpUid, String statusinvoice) {
        log.info("Zadel TkpService: receiveTkpStatusUpdate() вызван, tkpUid={}, statusinvoice={}", tkpUid, statusinvoice);
        TkpStatus tkpStatus = TkpStatus.builder()
                .tkpUid(tkpUid)
                .subStatus(statusinvoice)
                .datetime(ZonedDateTime.now())
                .build();
        tkpStatusRepository.save(tkpStatus);

        if ("accept".equals(statusinvoice)) {
            log.info("Zadel TkpService: обрабатываю accept для tkpUid={}", tkpUid);
            tkpListRepository.findById(tkpUid).ifPresent(tkp -> {
                String orderUid = tkp.getOrderUid();
                if (orderUid != null) {
                    OrderStatus orderStatus = OrderStatus.builder()
                            .orderUid(orderUid)
                            .status("processed")
                            .subStatus("accept")
                            .datetime(ZonedDateTime.now())
                            .build();
                    orderStatusRepository.save(orderStatus);
                    log.info("Zadel TkpService: OrderStatus accept сохранён, orderUid={}", orderUid);
                    
                    ordersListRepository.findById(orderUid).ifPresent(order -> {
                        order.setStatusreason("accept");
                        ordersListRepository.save(order);
                        log.info("Zadel TkpService: OrdersList statusreason=accept, orderUid={}", orderUid);
                    });
                }
            });
        }

        if ("inrealise".equals(statusinvoice)) {
            log.info("Zadel TkpService: обрабатываю inrealise для tkpUid={}", tkpUid);
            tkpListRepository.findById(tkpUid).ifPresent(tkp -> {
                String orderUid = tkp.getOrderUid();
                if (orderUid != null) {
                    OrderTracking tracking = OrderTracking.builder()
                            .orderUid(orderUid)
                            .trackingStatus("notinwork")
                            .datetime(ZonedDateTime.now())
                            .build();
                    orderTrackingRepository.save(tracking);
                    log.info("Zadel TkpService: OrderTracking notinwork сохранён, orderUid={}", orderUid);
                }
            });
        }

        tkpListRepository.findById(tkpUid).ifPresent(tkp -> {
            tkp.setStatusinvoice(statusinvoice);
            String orderUid = tkp.getOrderUid();
            
            if ("paid".equals(statusinvoice) || "unpaid".equals(statusinvoice) || 
                "cancelprovider".equals(statusinvoice) || "cancelcustomer".equals(statusinvoice)) {
                tkp.setStatus("closed");
                log.info("Zadel TkpService: закрываю ТКП, tkpUid={}", tkpUid);
                
                if (orderUid != null) {
                    String orderSubStatus = "paid".equals(statusinvoice) ? "done" : statusinvoice;
                    OrderStatus orderStatus = OrderStatus.builder()
                            .orderUid(orderUid)
                            .status("closed")
                            .subStatus(orderSubStatus)
                            .datetime(ZonedDateTime.now())
                            .build();
                    orderStatusRepository.save(orderStatus);
                    
                    ordersListRepository.findById(orderUid).ifPresent(order -> {
                        order.setStatus("closed");
                        order.setStatusreason(orderSubStatus);
                        ordersListRepository.save(order);
                    });
                }
            }
            tkpListRepository.save(tkp);
        });
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> sendTkp(String orderUid, Map<String, Object> request) {
        log.info("Zadel TkpService: sendTkp() вызван, orderUid={}, request={}", orderUid, request);
        Optional<Map<String, Object>> orderOpt = orderService.getOrder(orderUid);
        if (orderOpt.isEmpty()) {
            log.error("Zadel TkpService: заказ не найден: {}", orderUid);
            throw new RuntimeException("Заказ не найден: " + orderUid);
        }

        Map<String, Object> orderData = new LinkedHashMap<>(orderOpt.get());
        String tkpUid = UUID.randomUUID().toString();
        log.info("Zadel TkpService: сгенерирован tkpUid={}", tkpUid);
        
        Map<String, Double> prices = new HashMap<>();
        Map<String, String> zadelUids = new HashMap<>();
        Map<String, Double> relevanceScores = new HashMap<>();
        
        List<Map<String, Object>> priceList = (List<Map<String, Object>>) request.get("prices");
        log.info("Zadel TkpService: priceList={}", priceList);
        if (priceList != null) {
            for (Map<String, Object> priceItem : priceList) {
                String productUid = (String) priceItem.get("productUid");
                if (priceItem.get("price") != null) {
                    prices.put(productUid, Double.parseDouble(priceItem.get("price").toString()));
                }
                String zadelUid = (String) priceItem.get("zadelProductUid");
                if (zadelUid != null) {
                    zadelUids.put(productUid, zadelUid);
                    log.info("Zadel TkpService: маппинг AWMS {} -> Zadel {}", productUid, zadelUid);
                }
                if (priceItem.get("relevance") != null) {
                    relevanceScores.put(productUid, Double.parseDouble(priceItem.get("relevance").toString()));
                }
            }
        }

        List<Map<String, Object>> awmsProducts = (List<Map<String, Object>>) orderData.get("products");
        List<Map<String, Object>> newProducts = new ArrayList<>();
        double totalCost = 0;

        if (awmsProducts != null) {
            for (Map<String, Object> awmsProduct : awmsProducts) {
                String productUid = (String) awmsProduct.get("product_uid");
                String zadelUid = zadelUids.get(productUid);
                int quantity = Integer.parseInt(awmsProduct.get("quantity").toString());
                double price = prices.getOrDefault(productUid, 100.0);
                double cost = price * quantity;
                totalCost += cost;

                Map<String, Object> newProduct = new LinkedHashMap<>();
                newProduct.put("product_uid", productUid);
                newProduct.put("quantity", quantity);
                newProduct.put("price", price);
                newProduct.put("cost", cost);

                if (zadelUid != null) {
                    newProduct.put("zadel_product_uid", zadelUid);
                    try {
                        UUID zUid = UUID.fromString(zadelUid);
                        log.info("Zadel TkpService: ищу материал Zadel по uid={}", zUid);
                        SprMaterialDTO material = nomenclatureService.getMaterial(zUid);
                        log.info("Zadel TkpService: материал найден: {}", material.getName());
                        newProduct.put("product", material.getName() != null ? material.getName() : awmsProduct.get("product"));
                        newProduct.put("article", material.getArticle() != null ? material.getArticle() : "");
                        newProduct.put("description", material.getDescription() != null ? material.getDescription() : "");
                        newProduct.put("group", material.getTypeMainName() != null ? material.getTypeMainName() : "");
                        newProduct.put("type", material.getTypeProductName() != null ? material.getTypeProductName() : "");
                        newProduct.put("manufacturer", material.getManufacturerName() != null ? material.getManufacturerName() : "");
                        newProduct.put("country", material.getCountryName() != null ? material.getCountryName() : "");
                        newProduct.put("brand", material.getBrandName() != null ? material.getBrandName() : "");
                        newProduct.put("model", material.getModelOfBrandName() != null ? material.getModelOfBrandName() : "");
                    } catch (Exception e) {
                        log.error("Zadel TkpService: материал не найден: {}", zadelUid, e);
                        newProduct.put("product", awmsProduct.get("product"));
                        newProduct.put("article", awmsProduct.get("article"));
                        newProduct.put("description", awmsProduct.get("description"));
                        newProduct.put("group", awmsProduct.get("group"));
                        newProduct.put("type", awmsProduct.get("type"));
                        newProduct.put("manufacturer", awmsProduct.get("manufacturer"));
                        newProduct.put("country", awmsProduct.get("country"));
                        newProduct.put("brand", awmsProduct.get("brand"));
                        newProduct.put("model", awmsProduct.get("model"));
                    }
                } else {
                    log.warn("Zadel TkpService: zadelUid не найден для productUid={}", productUid);
                    newProduct.put("product", awmsProduct.get("product"));
                    newProduct.put("article", awmsProduct.get("article"));
                    newProduct.put("description", awmsProduct.get("description"));
                    newProduct.put("group", awmsProduct.get("group"));
                    newProduct.put("type", awmsProduct.get("type"));
                    newProduct.put("manufacturer", awmsProduct.get("manufacturer"));
                    newProduct.put("country", awmsProduct.get("country"));
                    newProduct.put("brand", awmsProduct.get("brand"));
                    newProduct.put("model", awmsProduct.get("model"));
                }

                if (awmsProduct.get("images") != null) newProduct.put("images", awmsProduct.get("images"));
                if (awmsProduct.get("draws") != null) newProduct.put("draws", awmsProduct.get("draws"));
                if (awmsProduct.get("barcode") != null) newProduct.put("barcode", awmsProduct.get("barcode"));
                if (awmsProduct.get("sku") != null) newProduct.put("sku", awmsProduct.get("sku"));
                if (awmsProduct.get("specifications") != null) newProduct.put("specifications", awmsProduct.get("specifications"));
                if (awmsProduct.get("analogues") != null) newProduct.put("analogues", awmsProduct.get("analogues"));

                if (relevanceScores.containsKey(productUid)) {
                    newProduct.put("relevance", relevanceScores.get(productUid));
                }

                newProducts.add(newProduct);
            }
        }

        Map<String, Object> tkpData = new LinkedHashMap<>();
        tkpData.put("tkp_uid", tkpUid);
        tkpData.put("order_uid", orderUid);
        tkpData.put("ordernumber", orderData.get("ordernumber"));
        tkpData.put("tkp_number", "TKP-" + System.currentTimeMillis());
        tkpData.put("tkp_data", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        tkpData.put("customer", orderData.get("customer"));
        tkpData.put("ogrn", orderData.get("ogrn"));
        tkpData.put("inn", orderData.get("inn"));
        tkpData.put("kpp", orderData.get("kpp"));
        tkpData.put("legaladdress", orderData.get("legaladdress"));
        tkpData.put("deliveryaddress", orderData.get("deliveryaddress"));
        tkpData.put("contactperson", orderData.get("contactperson"));
        tkpData.put("contact", orderData.get("contact"));
        tkpData.put("total_cost", request.getOrDefault("totalCost", totalCost));
        tkpData.put("delivery_date", request.getOrDefault("deliveryDate", LocalDate.now().plusDays(7).toString()));
        tkpData.put("products", newProducts);

        try {
            log.info("Zadel TkpService: отправляю ТКП в SAAS, url={}", saasServiceUrl + "/v1/tkp/" + tkpUid);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(tkpData, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/tkp/" + tkpUid, HttpMethod.POST, entity, String.class);
            log.info("Zadel TkpService: SAAS принял ТКП");

            saveTkpLocally(tkpUid, tkpData);

            TkpStatus tkpStatus = TkpStatus.builder()
                    .tkpUid(tkpUid)
                    .orderUid(orderUid)
                    .subStatus("unaccept")
                    .datetime(ZonedDateTime.now())
                    .build();
            tkpStatusRepository.save(tkpStatus);
            log.info("Zadel TkpService: локально сохранён unaccept");

            log.info("Zadel TkpService: отправляю unaccept в SAAS, url={}", saasServiceUrl + "/v1/tkp/" + tkpUid + "/statusinvoice");
            Map<String, Object> invoiceBody = Map.of("statusinvoice", "unaccept");
            HttpEntity<Map<String, Object>> invoiceEntity = new HttpEntity<>(invoiceBody, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/tkp/" + tkpUid + "/statusinvoice", HttpMethod.POST, invoiceEntity, String.class);
            log.info("Zadel TkpService: SAAS принял unaccept");

            log.info("Zadel TkpService: отправляю posttkpprovider в SAAS");
            Map<String, Object> reasonBody = Map.of("statusreason", "posttkpprovider");
            HttpEntity<Map<String, Object>> reasonEntity = new HttpEntity<>(reasonBody, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/orders/" + orderUid + "/statusreason", HttpMethod.POST, reasonEntity, String.class);
            log.info("Zadel TkpService: SAAS принял posttkpprovider");

            log.info("Zadel TkpService: отправляю processed в SAAS");
            Map<String, Object> statusBody = Map.of("status", "processed");
            HttpEntity<Map<String, Object>> statusEntity = new HttpEntity<>(statusBody, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/orders/" + orderUid + "/status", HttpMethod.POST, statusEntity, String.class);
            log.info("Zadel TkpService: SAAS принял processed");

            orderService.receiveStatusReasonUpdate(orderUid, "posttkpprovider");
            orderService.receiveStatusUpdate(orderUid, "processed");

        } catch (Exception e) {
            log.error("Zadel TkpService: ошибка отправки ТКП", e);
            throw new RuntimeException("Ошибка отправки ТКП: " + e.getMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tkp_uid", tkpUid);
        response.put("order_uid", orderUid);
        response.put("status", "sent");
        response.put("message", "ТКП отправлен в брокер");
        return response;
    }

    @Transactional
    public void addTrack(String orderUid, String statustrack) {
        log.info("Zadel TkpService: addTrack() вызван, orderUid={}, statustrack={}", orderUid, statustrack);
        OrderTracking tracking = OrderTracking.builder()
                .orderUid(orderUid)
                .trackingStatus(statustrack)
                .datetime(ZonedDateTime.now())
                .build();
        orderTrackingRepository.save(tracking);

        try {
            Map<String, Object> body = Map.of("statustrack", statustrack);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/orders/" + orderUid + "/statustrack", HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            log.error("Zadel TkpService: ошибка отправки трека", e);
        }
    }

    @Transactional
    public void cancelTkp(String tkpUid) {
        log.info("Zadel TkpService: cancelTkp() вызван, tkpUid={}", tkpUid);
        postStatusInvoice(tkpUid, "cancelprovider");
    }

    @Transactional
    public void payTkp(String tkpUid) {
        log.info("Zadel TkpService: payTkp() вызван, tkpUid={}", tkpUid);
        postStatusInvoice(tkpUid, "paid");
    }

    @Transactional
    public void confirmInrealise(String tkpUid) {
        log.info("Zadel TkpService: confirmInrealise() вызван, tkpUid={}", tkpUid);
        postStatusInvoice(tkpUid, "inrealise");
    }

    @Transactional
    public void completeTkp(String tkpUid) {
        log.info("Zadel TkpService: completeTkp() вызван, tkpUid={}", tkpUid);
        postStatusInvoice(tkpUid, "unpaid");
    }

    private void postStatusInvoice(String tkpUid, String statusinvoice) {
        log.info("Zadel TkpService: postStatusInvoice() вызван, tkpUid={}, statusinvoice={}", tkpUid, statusinvoice);
        try {
            Map<String, Object> body = Map.of("statusinvoice", statusinvoice);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/tkp/" + tkpUid + "/statusinvoice", HttpMethod.POST, entity, String.class);
            receiveTkpStatusUpdate(tkpUid, statusinvoice);
        } catch (Exception e) {
            log.error("Zadel TkpService: ошибка postStatusInvoice {}", tkpUid, e);
        }
    }

    private String getLatestTkpStatus(String tkpUid) {
        List<TkpStatus> statuses = tkpStatusRepository.findByTkpUidOrderByDatetimeDesc(tkpUid);
        String subStatus = statuses.stream().filter(s -> s.getSubStatus() != null).findFirst().map(TkpStatus::getSubStatus).orElse(null);
        return ("paid".equals(subStatus) || "unpaid".equals(subStatus) || "cancelcustomer".equals(subStatus) || "cancelprovider".equals(subStatus)) ? "closed" : "active";
    }

    private String getLatestStatusInvoice(String tkpUid) {
        List<TkpStatus> statuses = tkpStatusRepository.findByTkpUidOrderByDatetimeDesc(tkpUid);
        return statuses.stream().filter(s -> s.getSubStatus() != null).findFirst().map(TkpStatus::getSubStatus).orElse(null);
    }

    private Map<String, Object> toTkpListMap(TkpList tkp) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tkp_uid", tkp.getTkpUid());
        map.put("order_uid", tkp.getOrderUid());
        map.put("customer_id", tkp.getCustomerId());
        map.put("tkp_number", tkp.getOrderNumber());
        map.put("tkp_datetime", tkp.getOrderDatetime() != null ? tkp.getOrderDatetime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
        map.put("delivery_date", tkp.getDeliveryDate() != null ? tkp.getDeliveryDate().toString() : null);
        map.put("total_cost", tkp.getTotalCost());
        map.put("status", tkp.getStatus());
        map.put("statusinvoice", tkp.getStatusinvoice());
        
        // Добавляем номер заказа
        String orderUid = tkp.getOrderUid();
        if (orderUid != null) {
            ordersListRepository.findById(orderUid).ifPresent(order -> {
                map.put("order_number", order.getOrderNumber());
            });
        }
        
        return map;
    }

    private ZonedDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        try {
            return ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}