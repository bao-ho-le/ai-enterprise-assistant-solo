package com.enterprise.aiassistant.backend.ai.vectorstore.service;

import com.enterprise.aiassistant.backend.ai.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.VectorStoreException;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QdrantVectorStoreService implements VectorStoreService {

    private final QdrantClient qdrantClient;

    private final QdrantProperties properties;

    private final VectorStoreMapper vectorStoreMapper;

    private final VectorStoreHelper vectorStoreHelper;

    @Override
    public void upsert(VectorPoint point) {
        upsert(List.of(point));
    }

    @Override
    public void upsert(List<VectorPoint> points) {

        vectorStoreHelper.validateVectorPoints(points);

        try {

            List<Points.PointStruct> pointStructs = points.stream()
                    .map(vectorStoreMapper::toPointStruct)
                    .toList();

            Futures.getUnchecked(
                    qdrantClient.upsertAsync(properties.getCollectionName(), pointStructs)
            );

        } catch (Exception e) {
            throw new VectorStoreException(ErrorCode.VECTOR_UPSERT_FAILED, e);
        }
    }

    @Override
    public void delete(Long pointId) {

        vectorStoreHelper.validatePointId(pointId);

        try {

            Futures.getUnchecked(
                    qdrantClient.deleteAsync(
                            properties.getCollectionName(),
                            List.of(PointIdFactory.id(pointId))
                    )
            );

        } catch (Exception e) {
            throw new VectorStoreException(ErrorCode.VECTOR_DELETE_FAILED, e);
        }
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int limit, Long documentId) {

        vectorStoreHelper.validateSearchRequest(queryVector, limit, documentId);

        try {

            Points.SearchPoints.Builder request = Points.SearchPoints.newBuilder()
                    .setCollectionName(properties.getCollectionName())
                    .addAllVector(vectorStoreMapper.toFloatList(queryVector))
                    .setLimit(limit)
                    .setScoreThreshold(properties.getScoreThreshold())
                    .setWithPayload(WithPayloadSelectorFactory.enable(true));

            // Thêm filter, chỉ search trong 1 document nhất định nếu có truyền document id
            Points.Filter filter = vectorStoreHelper.buildDocumentFilter(documentId);

            if (filter != null) {
                request.setFilter(filter);
            }

            List<Points.ScoredPoint> scoredPoints =
                    Futures.getUnchecked(qdrantClient.searchAsync(request.build()));

            return vectorStoreMapper.toSearchResults(scoredPoints);

        } catch (Exception e) {
            throw new VectorStoreException(ErrorCode.VECTOR_SEARCH_FAILED, e);
        }
    }

}
