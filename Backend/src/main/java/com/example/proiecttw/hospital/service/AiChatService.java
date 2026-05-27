package com.example.proiecttw.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AiChatService {

    @Value("${ollama.baseUrl}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String ask(String userMessage, String context) {
        String prompt = """
        Ești un asistent virtual pentru o aplicație de programări la spital.

        REGULI STRICTE:
        - Răspunde DOAR pe baza informațiilor din CONTEXT.
        - Dacă informația nu e în CONTEXT, răspunde: "Nu am aceste date în sistem."
        - Nu inventa doctori, pacienți, date, ore sau programări.
        - Nu oferi diagnostic sau tratament.
        - Ajută doar cu: programări, doctori, specializări, disponibilitate.

        CONTEXT:
        %s

        ÎNTREBARE:
        %s
        """.formatted(context, userMessage);

        String body = """
        {
          "model": "%s",
          "prompt": %s,
          "stream": false
        }
        """.formatted(model, json(prompt));

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new RuntimeException("Ollama HTTP " + res.statusCode() + ": " + res.body());
            }

            JsonNode root = mapper.readTree(res.body());
            return root.path("response").asText("(No response)");

        } catch (Exception e) {
            throw new RuntimeException("Ollama error: " + e.getMessage(), e);
        }
    }

    private static String json(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }
}
