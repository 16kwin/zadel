// ЗАДЕЛ — controller/OrderController.java — ПОЛНЫЙ ФАЙЛ
package com.example.zadel.controller;

import com.example.zadel.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    @GetMapping("/closed")
    public ResponseEntity<List<Map<String, Object>>> getClosedOrders() {
        return ResponseEntity.ok(orderService.getClosedOrders());
    }

    @GetMapping("/{orderUid}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderUid) {
        return orderService.getOrder(orderUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Приём заказа от SAAS
    @PostMapping("/{orderUid}")
    public ResponseEntity<Map<String, Object>> receiveOrder(
            @PathVariable String orderUid,
            @RequestBody Map<String, Object> request) {
        
        orderService.receiveOrder(orderUid, request);
        
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("order_uid", orderUid);
        notification.put("order_number", request.getOrDefault("ordernumber", ""));
        notification.put("customer", request.getOrDefault("customer", ""));
        messagingTemplate.convertAndSend("/topic/orders/new", notification);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("order_uid", orderUid);
        response.put("status", "received");
        response.put("message", "Заказ получен");
        return ResponseEntity.status(201).body(response);
    }

    // Приём обновлений статуса от SAAS
    @PostMapping("/{orderUid}/status")
    public ResponseEntity<?> receiveStatus(@PathVariable String orderUid, @RequestBody Map<String, Object> request) {
        String status = (String) request.get("status");
        if (status != null) {
            orderService.receiveStatusUpdate(orderUid, status);
        }
        return ResponseEntity.ok().build();
    }

    // Приём обновлений statusreason от SAAS
    @PostMapping("/{orderUid}/statusreason")
    public ResponseEntity<?> receiveStatusReason(@PathVariable String orderUid, @RequestBody Map<String, Object> request) {
        String statusreason = (String) request.get("statusreason");
        if (statusreason != null) {
            orderService.receiveStatusReasonUpdate(orderUid, statusreason);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancelled")
    public ResponseEntity<?> orderCancelled(@RequestBody Map<String, Object> request) {
        String orderUid = (String) request.get("order_uid");
        if (orderUid != null) {
            orderService.markOrderCancelledByCustomer(orderUid);
        }
        messagingTemplate.convertAndSend("/topic/orders/cancelled", request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderUid}/take-to-work")
    public ResponseEntity<?> takeToWork(@PathVariable String orderUid) {
        orderService.takeToWork(orderUid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderUid}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderUid) {
        orderService.cancelOrder(orderUid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderUid}/track")
    public ResponseEntity<Map<String, Object>> addTrack(
            @PathVariable String orderUid,
            @RequestBody Map<String, Object> request) {
        String statustrack = (String) request.get("statustrack");
        return ResponseEntity.ok(orderService.addTrack(orderUid, statustrack));
    }
}