package org.example.pspotl.biztalkwebhook.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    @PostMapping("/report")
    public ResponseEntity<String> receiveReport(@RequestBody String reportPayload) {
        log.info("[WEBHOOK RECEIVED] Payload: {}", reportPayload);
        // DB 전송 결과 업데이트 로직
        return ResponseEntity.ok("ACK");
    }
}
