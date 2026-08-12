package com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.dto.VectorPoint;

import java.util.List;

public interface VectorStoreService {

    // Hỗ trợ upsert 1 hoặc nhiều point
    void upsert(VectorPoint point);

    void upsert(List<VectorPoint> points);

    // Xoá
    void delete(Long pointId);

    // Nếu document id = null thì nghĩa là search trên toàn qdrant
    // Nếu có document id cụ thể thì chỉ search trên document đó thôi
    default List<SearchResult> search(float[] queryVector, int limit) {
        return search(queryVector, limit, null);
    }

    List<SearchResult> search(
            float[] queryVector,
            int limit,
            Long documentId
    );

}