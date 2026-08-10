package com.enterprise.aiassistant.backend.ai.vectorstore.helper;

import com.enterprise.aiassistant.backend.ai.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class VectorStoreHelper {

    private final QdrantProperties properties;

    public void validateVectorPoints(List<VectorPoint> points) {

        if (points == null) {
            throw new BusinessException(ErrorCode.VECTOR_POINTS_REQUIRED);
        }

        if (points.isEmpty()) {
            return;
        }

        for (VectorPoint point : points) {

            if (point == null) {
                throw new BusinessException(ErrorCode.VECTOR_POINT_REQUIRED);
            }

            if (point.getId() == null) {
                throw new BusinessException(ErrorCode.VECTOR_POINT_ID_REQUIRED);
            }

            float[] vector = point.getVector();

            if (vector == null || vector.length == 0) {
                throw new BusinessException(
                        ErrorCode.VECTOR_REQUIRED,
                        ErrorCode.VECTOR_REQUIRED.getMessage() + point.getId()
                );
            }

            if (vector.length != properties.getVectorSize()) {
                throw new BusinessException(
                        ErrorCode.VECTOR_DIMENSION_INVALID,
                        String.format(
                                "Invalid vector dimension for point %d. Expected %d but got %d.",
                                point.getId(),
                                properties.getVectorSize(),
                                vector.length
                        )
                );
            }
        }
    }

    public void validatePointId(Long pointId) {
        if (pointId == null) {
            throw new BusinessException(ErrorCode.VECTOR_POINT_ID_REQUIRED);
        }

        if (pointId <= 0) {
            throw new BusinessException(
                    ErrorCode.VECTOR_POINT_ID_INVALID,
                    "Point id must be greater than 0."
            );
        }
    }

    public void validateSearchRequest(
            float[] queryVector,
            int limit,
            Long documentId
    ) {

        if (queryVector == null || queryVector.length == 0) {
            throw new BusinessException(ErrorCode.VECTOR_REQUIRED);
        }

        if (queryVector.length != properties.getVectorSize()) {
            throw new BusinessException(
                    ErrorCode.VECTOR_DIMENSION_INVALID,
                    String.format(
                            "Invalid query vector dimension. Expected %d but got %d.",
                            properties.getVectorSize(),
                            queryVector.length
                    )
            );
        }

        if (limit <= 0) {
            throw new BusinessException(
                    ErrorCode.VECTOR_SEARCH_LIMIT_INVALID
            );
        }

        if (documentId != null && documentId <= 0) {
            throw new DocumentException(ErrorCode.DOCUMENT_ID_INVALID);
        }
    }

    public Points.Filter buildDocumentFilter(Long documentId) {

        if (documentId == null) {
            return null;
        }

        return Points.Filter.newBuilder()
                .addMust(ConditionFactory.match("documentId", documentId))
                .build();
    }

    public void putLong(Map<String, JsonWithInt.Value> map, String key, Long value) {
        if (value != null) {
            map.put(key, ValueFactory.value(value));
        }
    }

    public void putInt(Map<String, JsonWithInt.Value> map, String key, Integer value) {
        if (value != null) {
            map.put(key, ValueFactory.value(value.longValue()));
        }
    }

    public void putString(Map<String, JsonWithInt.Value> map, String key, String value) {
        if (value != null) {
            map.put(key, ValueFactory.value(value));
        }
    }

    public Long getLong(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value value = payload.get(key);
        return (value != null && value.hasIntegerValue()) ? value.getIntegerValue() : null;
    }

    public Integer getInt(Map<String, JsonWithInt.Value> payload, String key) {
        Long value = getLong(payload, key);
        return value != null ? value.intValue() : null;
    }

    public String getString(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value value = payload.get(key);
        return (value != null && value.hasStringValue()) ? value.getStringValue() : null;
    }
}