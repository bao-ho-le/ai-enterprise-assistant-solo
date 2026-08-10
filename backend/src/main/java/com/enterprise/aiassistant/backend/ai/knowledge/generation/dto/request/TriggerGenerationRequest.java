package com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class TriggerGenerationRequest {

    // Not JsonNode: Spring here deserializes @RequestBody with its auto-configured
    // Jackson 3 (tools.jackson) converter, which doesn't know how to build a Jackson 2
    // (com.fasterxml.jackson) JsonNode. Map deserializes fine under either Jackson
    // version; GenerationHelper converts it to a Jackson 2 JsonNode afterwards.
    @NotNull
    private Map<String, Object> inputData;
}
