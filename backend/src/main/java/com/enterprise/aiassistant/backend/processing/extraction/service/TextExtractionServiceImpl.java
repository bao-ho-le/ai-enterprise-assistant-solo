package com.enterprise.aiassistant.backend.processing.extraction.service;

import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.extractor.TextExtractor;
import com.enterprise.aiassistant.backend.processing.extraction.extractor.TextExtractorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextExtractionServiceImpl implements TextExtractionService {

    private final TextExtractorFactory extractorFactory;


    @Override
    public ExtractedText extract(
            Resource resource,
            String mimeType
    ) {

        TextExtractor extractor =
                extractorFactory.getExtractor(mimeType);


        return extractor.extract(resource);
    }

}
