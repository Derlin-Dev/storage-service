package com.backend.storage_service.debug;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file-store/api/v1/debug")
public class DebugController {

    @GetMapping("/auth-info")
    public ResponseEntity<?> getAuthInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        info.put("authHeader", request.getHeader("Authorization"));
        info.put("userId", request.getAttribute("userId"));
        info.put("authenticated", request.getUserPrincipal() != null);
        info.put("requestUri", request.getRequestURI());
        info.put("method", request.getMethod());
        return ResponseEntity.ok(info);
    }
}
