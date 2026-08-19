// ЗАДЕЛ — service/OrderService.java — ПОЛНЫЙ ФАЙЛ С ЛОГАМИ + формат даты
package com.example.zadel.service;

import com.example.zadel.model.*;
import com.example.zadel.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrdersListRepository ordersListRepository;
    private final OrdersFullRepository ordersFullRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final TkpListRepository tkpListRepository;
    private final TkpStatusRepository tkpStatusRepository;
    private final RestTemplate restTemplate;
    private final SimpMessagingTemplate messagingTemplate;
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

    public List<Map<String, Object>> getActiveOrders() {
        log.info("Zadel OrderService: getActiveOrders() вызван");
        return ordersListRepository.findByStatusIn(Arrays.asList("active", "processed"))
                .stream().map(this::toOrderListMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getClosedOrders() {
        log.info("Zadel OrderService: getClosedOrders() вызван");
        return ordersListRepository.findByStatus("closed")
                .stream().map(this::toOrderListMap).collect(Collectors.toList());
    }

    public Optional<Map<String, Object>> getOrder(String orderUid) {
        log.info("Zadel OrderService: getOrder() вызван, orderUid={}", orderUid);
        Optional<OrdersFull> fullOpt = ordersFullRepository.findByOrderUid(orderUid);
        if (fullOpt.isPresent()) {
            try {
                Map<String, Object> order = objectMapper.readValue(fullOpt.get().getOrderJson(), Map.class);
                order.put("statustrack", getLatestTrackingStatus(orderUid));
                order.put("status", getLatestStatus(orderUid));
                order.put("statusreason", getLatestStatusReason(orderUid));
                log.info("Zadel OrderService: заказ {}, status={}, statusreason={}, statustrack={}", 
                    orderUid, order.get("status"), order.get("statusreason"), order.get("statustrack"));
                
                String currentReason = getLatestStatusReason(orderUid);
                if ("cancelcustomer".equals(currentReason) || "cancelprovider".equals(currentReason)) {
                    List<OrderStatus> statuses = orderStatusRepository.findByOrderUidOrderByDatetimeDesc(orderUid);
                    String prevReason = statuses.stream()
                        .map(OrderStatus::getSubStatus)
                        .filter(s -> s != null && !s.equals("cancelcustomer") && !s.equals("cancelprovider"))
                        .findFirst()
                        .orElse(null);
                    
                    if ("posttkpprovider".equals(prevReason)) {
                        List<TkpList> tkpList = tkpListRepository.findByOrderUid(orderUid);
                        for (TkpList tkp : tkpList) {
                            List<TkpStatus> tkpStatuses = tkpStatusRepository.findByTkpUidOrderByDatetimeDesc(tkp.getTkpUid());
                            boolean tkpAccepted = tkpStatuses.stream()
                                .anyMatch(s -> "accept".equals(s.getSubStatus()) 
                                    || "inrealise".equals(s.getSubStatus())
                                    || "paid".equals(s.getSubStatus())
                                    || "unpaid".equals(s.getSubStatus()));
                            if (tkpAccepted) {
                                prevReason = "accept";
                                break;
                            }
                        }
                    }
                    
                    order.put("previous_statusreason", prevReason);
                    log.info("Zadel OrderService: заказ {} отменён, previous_statusreason={}", orderUid, prevReason);
                }
                
                return Optional.of(order);
            } catch (Exception e) {
                log.error("Zadel OrderService: ошибка парсинга заказа {}", orderUid, e);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void saveOrderLocally(String orderUid, Map<String, Object> orderData, String status, String statusreason) {
        log.info("Zadel OrderService: saveOrderLocally() вызван, orderUid={}, status={}, reason={}", orderUid, status, statusreason);
        OrdersList orderList = OrdersList.builder()
                .orderUid(orderUid)
                .customerId((String) orderData.get("customer"))
                .orderNumber((String) orderData.get("ordernumber"))
                .orderDatetime(parseDateTime((String) orderData.get("orderdata")))
                .status(status)
                .statusreason(statusreason)
                .syncedAt(ZonedDateTime.now())
                .build();
        ordersListRepository.save(orderList);
        log.info("Zadel OrderService: OrdersList сохранён");

        try {
            String json = objectMapper.writeValueAsString(orderData);
            OrdersFull orderFull = OrdersFull.builder()
                    .orderUid(orderUid)
                    .orderJson(json)
                    .build();
            ordersFullRepository.save(orderFull);
            log.info("Zadel OrderService: OrdersFull сохранён");
        } catch (Exception e) {
            log.error("Zadel OrderService: ошибка сохранения OrdersFull {}", orderUid, e);
        }
    }

    @Transactional
    public void receiveOrder(String orderUid, Map<String, Object> request) {
        log.info("Zadel OrderService: receiveOrder() вызван, orderUid={}", orderUid);
        saveOrderLocally(orderUid, request, "active", "inprocessing");

        OrderStatus orderStatus = OrderStatus.builder()
                .orderUid(orderUid)
                .status("active")
                .subStatus("inprocessing")
                .datetime(ZonedDateTime.now())
                .build();
        orderStatusRepository.save(orderStatus);
        log.info("Zadel OrderService: OrderStatus active/inprocessing сохранён");
        
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("order_uid", orderUid);
        notification.put("order_number", request.getOrDefault("ordernumber", ""));
        notification.put("customer", request.getOrDefault("customer", ""));
        messagingTemplate.convertAndSend("/topic/orders/new", notification);
    }

    @Transactional
    public void receiveStatusUpdate(String orderUid, String newStatus) {
        log.info("Zadel OrderService: receiveStatusUpdate() вызван, orderUid={}, newStatus={}", orderUid, newStatus);
        OrderStatus orderStatus = OrderStatus.builder()
                .orderUid(orderUid)
                .status(newStatus)
                .datetime(ZonedDateTime.now())
                .build();
        orderStatusRepository.save(orderStatus);
        log.info("Zadel OrderService: OrderStatus status сохранён");

        ordersListRepository.findById(orderUid).ifPresent(order -> {
            order.setStatus(newStatus);
            ordersListRepository.save(order);
            log.info("Zadel OrderService: OrdersList status обновлён");
        });
        
        messagingTemplate.convertAndSend("/topic/orders/refresh", Map.of("order_uid", orderUid, "type", "status_update"));
    }

    @Transactional
    public void receiveStatusReasonUpdate(String orderUid, String statusreason) {
        log.info("Zadel OrderService: receiveStatusReasonUpdate() вызван, orderUid={}, statusreason={}", orderUid, statusreason);
        OrderStatus orderStatus = OrderStatus.builder()
                .orderUid(orderUid)
                .subStatus(statusreason)
                .datetime(ZonedDateTime.now())
                .build();
        orderStatusRepository.save(orderStatus);
        log.info("Zadel OrderService: OrderStatus subStatus сохранён");

        ordersListRepository.findById(orderUid).ifPresent(order -> {
            order.setStatusreason(statusreason);
            ordersListRepository.save(order);
            log.info("Zadel OrderService: OrdersList statusreason обновлён");
        });
        
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("order_uid", orderUid);
        notification.put("type", "statusreason_update");
        
        if ("cancelcustomer".equals(statusreason) || "cancelprovider".equals(statusreason)) {
            notification.put("type", "order_cancelled");
            messagingTemplate.convertAndSend("/topic/orders/cancelled", notification);
        }
        
        messagingTemplate.convertAndSend("/topic/orders/refresh", notification);
    }

    @Transactional
    public void takeToWork(String orderUid) {
        log.info("Zadel OrderService: takeToWork() вызван, orderUid={}", orderUid);
        try {
            Map<String, Object> body = Map.of("statusreason", "inworkprovider");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createJsonHeaders());
            log.info("Zadel OrderService: отправляю inworkprovider в SAAS");
            restTemplate.exchange(saasServiceUrl + "/v1/orders/" + orderUid + "/statusreason", HttpMethod.POST, entity, String.class);
            log.info("Zadel OrderService: SAAS принял inworkprovider");
            receiveStatusReasonUpdate(orderUid, "inworkprovider");
        } catch (Exception e) {
            log.error("Zadel OrderService: ошибка takeToWork {}", orderUid, e);
        }
    }

    @Transactional
    public void cancelOrder(String orderUid) {
        log.info("Zadel OrderService: cancelOrder() вызван, orderUid={}", orderUid);
        try {
            Map<String, Object> reasonBody = Map.of("statusreason", "cancelprovider");
            HttpEntity<Map<String, Object>> reasonEntity = new HttpEntity<>(reasonBody, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/orders/" + orderUid + "/statusreason", HttpMethod.POST, reasonEntity, String.class);

            Map<String, Object> statusBody = Map.of("status", "closed");
            HttpEntity<Map<String, Object>> statusEntity = new HttpEntity<>(statusBody, createJsonHeaders());
            restTemplate.exchange(saasServiceUrl + "/v1/orders/" + orderUid + "/status", HttpMethod.POST, statusEntity, String.class);

            receiveStatusReasonUpdate(orderUid, "cancelprovider");
            receiveStatusUpdate(orderUid, "closed");
        } catch (Exception e) {
            log.error("Zadel OrderService: ошибка cancelOrder {}", orderUid, e);
        }
    }

    @Transactional
    public void markOrderCancelledByCustomer(String orderUid) {
        log.info("Zadel OrderService: markOrderCancelledByCustomer() вызван, orderUid={}", orderUid);
        receiveStatusReasonUpdate(orderUid, "cancelcustomer");
        receiveStatusUpdate(orderUid, "closed");
    }

    @Transactional
    public Map<String, Object> addTrack(String orderUid, String statustrack) {
        log.info("Zadel OrderService: addTrack() вызван, orderUid={}, statustrack={}", orderUid, statustrack);
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
            log.error("Zadel OrderService: ошибка addTrack {}", orderUid, e);
        }
        
        messagingTemplate.convertAndSend("/topic/orders/refresh", Map.of("order_uid", orderUid, "type", "track_update"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("order_uid", orderUid);
        response.put("statustrack", statustrack);
        response.put("message", "Трек обновлён");
        return response;
    }

    private String getLatestStatus(String orderUid) {
        List<OrderStatus> statuses = orderStatusRepository.findByOrderUidOrderByDatetimeDesc(orderUid);
        return statuses.stream().filter(s -> s.getStatus() != null).findFirst().map(OrderStatus::getStatus).orElse("active");
    }

    private String getLatestStatusReason(String orderUid) {
        List<OrderStatus> statuses = orderStatusRepository.findByOrderUidOrderByDatetimeDesc(orderUid);
        return statuses.stream().filter(s -> s.getSubStatus() != null).findFirst().map(OrderStatus::getSubStatus).orElse("inprocessing");
    }

    private String getLatestTrackingStatus(String orderUid) {
        List<OrderTracking> tracks = orderTrackingRepository.findByOrderUidOrderByDatetimeDesc(orderUid);
        return tracks.stream().filter(t -> t.getTrackingStatus() != null).findFirst().map(OrderTracking::getTrackingStatus).orElse(null);
    }

    private Map<String, Object> toOrderListMap(OrdersList order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("order_uid", order.getOrderUid());
        map.put("customer_id", order.getCustomerId());
        map.put("order_number", order.getOrderNumber());
        map.put("order_datetime", order.getOrderDatetime() != null
                ? order.getOrderDatetime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy-HH:mm")) : null);
        map.put("status", order.getStatus());
        map.put("statusreason", order.getStatusreason());
        map.put("statustrack", getLatestTrackingStatus(order.getOrderUid()));
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