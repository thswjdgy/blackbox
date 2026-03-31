package com.blackbox.domain.demo.controller;

import com.blackbox.domain.demo.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    @PostMapping("/seed")
    public ResponseEntity<String> seedDemoData() {
        demoService.seedData();
        return ResponseEntity.ok("Demo data successfully seeded.");
    }
}
