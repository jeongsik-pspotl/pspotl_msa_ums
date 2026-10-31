package org.example.pspotl.biztalkmockserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
@Slf4j
@RestController
@RequestMapping("/v1/external")
public class MockExternalApiController {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @PostMapping("/send/alimtalk")
    public CompletableFuture<ResponseEntity<String>> sendAlimtalk(@RequestBody Object payload) {
        CompletableFuture<ResponseEntity<String>> future = new CompletableFuture<>();
        int delay = ThreadLocalRandom.current().nextInt(5, 15);

        scheduler.schedule(() -> {
            // 5% 확률로 500 에러 발생
            if (ThreadLocalRandom.current().nextInt(100) < 5) {
                future.complete(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("VENDOR_ERROR"));
            } else {
                future.complete(ResponseEntity.ok("SUCCESS"));
            }
        }, delay, TimeUnit.MILLISECONDS);

        return future;
    }
}
