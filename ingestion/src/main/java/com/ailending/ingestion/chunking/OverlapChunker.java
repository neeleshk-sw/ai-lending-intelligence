package com.ailending.ingestion.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Word-based sliding-window chunker with configurable overlap.
 * Splits text into words, then produces chunks of `chunkSize` words
 * stepping forward by (chunkSize - overlapSize) each iteration.
 */
@Component
public class OverlapChunker implements Chunker {

    @Override
    public List<DocumentChunk> chunk(String text, ChunkConfig config) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] words = text.trim().split("\\s+");
        int step = config.getChunkSize() - config.getOverlapSize();
        List<DocumentChunk> chunks = new ArrayList<>();
        int sequence = 0;

        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + config.getChunkSize(), words.length);
            String chunkText = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));
            chunks.add(new DocumentChunk(chunkText, sequence++));
            if (end == words.length) break;
        }

        return chunks;
    }
}
