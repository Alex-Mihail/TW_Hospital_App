package com.example.proiecttw.hospital.dto;

import java.util.List;

public record ChatRequest(
        String message,
        String role,
        Long userId,
        UiContext uiContext
) {
    public record UiContext(
            String page,
            List<String> actions,
            List<String> steps
    ) {}
}
