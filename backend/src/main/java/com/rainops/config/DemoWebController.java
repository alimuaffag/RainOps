package com.rainops.config;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Profile("demo")
public class DemoWebController {
    @GetMapping({"/incidents", "/incidents/new", "/incidents/{id}", "/incidents/{id}/edit"})
    String application() {
        return "forward:/index.html";
    }

    @GetMapping("/health")
    @ResponseBody
    Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api/v1/csrf")
    @ResponseBody
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
