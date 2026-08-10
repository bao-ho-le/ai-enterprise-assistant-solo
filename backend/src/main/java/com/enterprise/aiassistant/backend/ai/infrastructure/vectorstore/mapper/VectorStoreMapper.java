package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPayload;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VectorStoreMapper {

    private final VectorStoreHelper vectorStoreHelper;

    public List<SearchResult> toSearchResults(List<Points.ScoredPoint> scoredPoints) {
        return scoredPoints.stream()
                .map(this::toSearchResult)
                .toList();
    }

    public Map<String, JsonWithInt.Value> toQdrantPayload(VectorPayload payload) {
        Map<String, JsonWithInt.Value> map = new HashMap<>();
        vectorStoreHelper.putLong(map, "chunkId", payload.getChunkId());
        vectorStoreHelper.putLong(map, "documentId", payload.getDocumentId());
        vectorStoreHelper.putLong(map, "documentVersionId", payload.getDocumentVersionId());
        vectorStoreHelper.putInt(map, "chunkIndex", payload.getChunkIndex());
        vectorStoreHelper.putInt(map, "pageNumber", payload.getPageNumber());
        vectorStoreHelper.putInt(map, "startChar", payload.getStartChar());
        vectorStoreHelper.putInt(map, "endChar", payload.getEndChar());
        vectorStoreHelper.putInt(map, "tokenCount", payload.getTokenCount());
        vectorStoreHelper.putString(map, "embeddingModel", payload.getEmbeddingModel());
        vectorStoreHelper.putString(map, "content", payload.getContent());
        return map;
    }

    public VectorPayload toVectorPayload(Map<String, JsonWithInt.Value> payload) {
        return VectorPayload.builder()
                .chunkId(vectorStoreHelper.getLong(payload, "chunkId"))
                .documentId(vectorStoreHelper.getLong(payload, "documentId"))
                .documentVersionId(vectorStoreHelper.getLong(payload, "documentVersionId"))
                .chunkIndex(vectorStoreHelper.getInt(payload, "chunkIndex"))
                .pageNumber(vectorStoreHelper.getInt(payload, "pageNumber"))
                .startChar(vectorStoreHelper.getInt(payload, "startChar"))
                .endChar(vectorStoreHelper.getInt(payload, "endChar"))
                .tokenCount(vectorStoreHelper.getInt(payload, "tokenCount"))
                .embeddingModel(vectorStoreHelper.getString(payload, "embeddingModel"))
                .content(vectorStoreHelper.getString(payload, "content"))
                .build();
    }

    public Points.PointStruct toPointStruct(VectorPoint point) {
        return Points.PointStruct.newBuilder()
                .setId(PointIdFactory.id(point.getId()))
                .setVectors(VectorsFactory.vectors(point.getVector()))
                .putAllPayload(toQdrantPayload(point.getPayload()))
                .build();
    }

    public SearchResult toSearchResult(Points.ScoredPoint scoredPoint) {
        return SearchResult.builder()
                .pointId(scoredPoint.getId().getNum())
                .score((double) scoredPoint.getScore())
                .payload(toVectorPayload(scoredPoint.getPayloadMap()))
                .build();
    }

    public List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
