package com.ailending.app.controller.dto.query;

import com.ailending.aicore.rag.RagResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP response body for {@code POST /api/lending/query}.
 *
 * <p>All fields required by NFR-5 are present:
 * generated text, confidence indicator, source references (hits), and duration.
 */
public class QueryResponse {

    private final String generatedText;
    private final double confidenceScore;
    private final String model;
    private final long totalDurationMs;
    private final List<HitDto> hits;

    private QueryResponse(String generatedText, double confidenceScore, String model,
                          long totalDurationMs, List<HitDto> hits) {
        this.generatedText  = generatedText;
        this.confidenceScore = confidenceScore;
        this.model           = model;
        this.totalDurationMs = totalDurationMs;
        this.hits            = hits;
    }

    public static QueryResponse from(RagResponse response) {
        List<HitDto> hitDtos = response.getSourceReferences().stream()
                .map(HitDto::from)
                .collect(Collectors.toList());
        return new QueryResponse(
                response.getAnswer(),
                response.getConfidenceScore(),
                response.getModelUsed(),
                response.getDurationMs(),
                hitDtos
        );
    }

    public String       getGeneratedText()   { return generatedText; }
    public double       getConfidenceScore() { return confidenceScore; }
    public String       getModel()           { return model; }
    public long         getTotalDurationMs() { return totalDurationMs; }
    public List<HitDto> getHits()            { return hits; }
}
