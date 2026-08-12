package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;


@Data
@Builder
@Getter
public class VectorPoint {

    private Long id;

    private float[] vector;

    private VectorPayload payload;

}