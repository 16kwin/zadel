// ЗАДЕЛ — controller/TkpController.java — ПОЛНЫЙ ФАЙЛ (добавлен WebSocket)
package com.example.zadel.controller;

import com.example.zadel.service.TkpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tkp")
@RequiredArgsConstructor
public class TkpController {

    private final TkpService tkpService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/outgoing")
    public ResponseEntity<List<Map<String, Object>>> getOutgoingTkp() {
        return ResponseEntity.ok(tkpService.getOutgoingTkp());
    }

    @GetMapping("/closed")
    public ResponseEntity<List<Map<String, Object>>> getClosedTkp() {
        return ResponseEntity.ok(tkpService.getClosedTkp());
    }

    @GetMapping("/{tkpUid}")
    public ResponseEntity<Map<String, Object>> getTkp(@PathVariable String tkpUid) {
        return tkpService.getTkp(tkpUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Приём ТКП от SAAS
    @PostMapping("/{tkpUid}")
    public ResponseEntity<?> receiveTkp(@PathVariable String tkpUid, @RequestBody Map<String, Object> request) {
        tkpService.receiveTkp(tkpUid, request);
        
        // WebSocket уведомление о новом ТКП
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("tkp_uid", tkpUid);
        notification.put("order_uid", request.get("order_uid"));
        messagingTemplate.convertAndSend("/topic/tkp/new", notification);
        messagingTemplate.convertAndSend("/topic/orders/refresh", notification);
        
        return ResponseEntity.ok().build();
    }

    // Приём обновлений статуса ТКП от SAAS
    @PostMapping("/{tkpUid}/statusinvoice")
    public ResponseEntity<?> receiveTkpStatus(@PathVariable String tkpUid, @RequestBody Map<String, Object> request) {
        String statusinvoice = (String) request.get("statusinvoice");
        if (statusinvoice != null) {
            tkpService.receiveTkpStatusUpdate(tkpUid, statusinvoice);
            
            // WebSocket уведомление об изменении статуса ТКП
            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("tkp_uid", tkpUid);
            notification.put("statusinvoice", statusinvoice);
            
            if ("accept".equals(statusinvoice)) {
                messagingTemplate.convertAndSend("/topic/tkp/accepted", notification);
            } else if ("cancelcustomer".equals(statusinvoice) || "cancelprovider".equals(statusinvoice)) {
                messagingTemplate.convertAndSend("/topic/tkp/cancelled", notification);
            }
            
            messagingTemplate.convertAndSend("/topic/tkp/status", notification);
            messagingTemplate.convertAndSend("/topic/orders/refresh", notification);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderUid}/send")
    public ResponseEntity<Map<String, Object>> sendTkp(
            @PathVariable String orderUid,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = tkpService.sendTkp(orderUid, request);
        
        // WebSocket уведомление о новом ТКП
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("tkp_uid", result.get("tkp_uid"));
        notification.put("order_uid", orderUid);
        messagingTemplate.convertAndSend("/topic/tkp/new", notification);
        messagingTemplate.convertAndSend("/topic/orders/refresh", notification);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/accepted")
    public ResponseEntity<?> tkpAccepted(@RequestBody Map<String, Object> request) {
        messagingTemplate.convertAndSend("/topic/tkp/accepted", request);
        messagingTemplate.convertAndSend("/topic/orders/refresh", request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancelled")
    public ResponseEntity<?> tkpCancelled(@RequestBody Map<String, Object> request) {
        messagingTemplate.convertAndSend("/topic/tkp/cancelled", request);
        messagingTemplate.convertAndSend("/topic/orders/refresh", request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderUid}/track")
    public ResponseEntity<?> addTrack(@PathVariable String orderUid, @RequestBody Map<String, Object> request) {
        String statustrack = (String) request.get("statustrack");
        tkpService.addTrack(orderUid, statustrack);
        
        // WebSocket уведомление об обновлении трека
        messagingTemplate.convertAndSend("/topic/orders/refresh", 
            Map.of("order_uid", orderUid, "type", "track_update"));
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tkpUid}/cancel")
    public ResponseEntity<?> cancelTkp(@PathVariable String tkpUid) {
        tkpService.cancelTkp(tkpUid);
        
        // WebSocket уведомление об отмене ТКП
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("tkp_uid", tkpUid);
        notification.put("statusinvoice", "cancelprovider");
        messagingTemplate.convertAndSend("/topic/tkp/cancelled", notification);
        messagingTemplate.convertAndSend("/topic/tkp/status", notification);
        messagingTemplate.convertAndSend("/topic/orders/refresh", notification);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tkpUid}/pay")
    public ResponseEntity<?> payTkp(@PathVariable String tkpUid) {
        tkpService.payTkp(tkpUid);
        
        messagingTemplate.convertAndSend("/topic/tkp/status", Map.of("tkp_uid", tkpUid, "statusinvoice", "paid"));
        messagingTemplate.convertAndSend("/topic/orders/refresh", Map.of("type", "tkp_status_update"));
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tkpUid}/inrealise")
    public ResponseEntity<?> confirmInrealise(@PathVariable String tkpUid) {
        tkpService.confirmInrealise(tkpUid);
        
        messagingTemplate.convertAndSend("/topic/tkp/status", Map.of("tkp_uid", tkpUid, "statusinvoice", "inrealise"));
        messagingTemplate.convertAndSend("/topic/orders/refresh", Map.of("type", "tkp_status_update"));
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tkpUid}/unpaid")
    public ResponseEntity<?> completeTkp(@PathVariable String tkpUid) {
        tkpService.completeTkp(tkpUid);
        
        messagingTemplate.convertAndSend("/topic/tkp/status", Map.of("tkp_uid", tkpUid, "statusinvoice", "unpaid"));
        messagingTemplate.convertAndSend("/topic/orders/refresh", Map.of("type", "tkp_status_update"));
        
        return ResponseEntity.ok().build();
    }
}