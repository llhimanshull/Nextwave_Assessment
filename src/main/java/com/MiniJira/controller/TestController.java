package com.minijira.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminOnly() {
        return ResponseEntity.ok(Map.of("message", "Success! You have ADMIN access."));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, String>> managerOnly() {
        return ResponseEntity.ok(Map.of("message", "Success! You have MANAGER access."));
    }

    @GetMapping("/member")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Map<String, String>> memberOnly() {
        return ResponseEntity.ok(Map.of("message", "Success! You have MEMBER access."));
    }

    @GetMapping("/authenticated")
    public ResponseEntity<Map<String, String>> anyAuthenticatedUser() {
        return ResponseEntity.ok(Map.of("message", "Success! You are authenticated (Any Role)."));
    }
}
