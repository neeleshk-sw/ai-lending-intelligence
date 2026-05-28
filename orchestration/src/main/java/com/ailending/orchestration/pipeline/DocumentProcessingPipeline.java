package com.ailending.orchestration.pipeline;

import com.ailending.ingestion.service.IngestionRequest;

public interface DocumentProcessingPipeline {

    DocumentProcessingResult process(IngestionRequest request);
}
