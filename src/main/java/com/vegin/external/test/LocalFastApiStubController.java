package com.vegin.external.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mock")
@Profile("local")
public class LocalFastApiStubController {
    private static final Logger log = LoggerFactory.getLogger(LocalFastApiStubController.class);

    @PostMapping("/analysis")
    public ResponseEntity<Void> stub(@RequestBody Map<String, Object> body) {
        log.info("[FASTAPI-STUB] /mock/analysis hit. payload={}", body);

        return ResponseEntity.accepted().build(); // 202
    }
}