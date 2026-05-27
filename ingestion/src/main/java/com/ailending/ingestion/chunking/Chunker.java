package com.ailending.ingestion.chunking;

import java.util.List;

/**
 * Interface for splitting document text into chunks suitable for embedding.
 */
public interface Chunker {

    /**
     * Splits the given text into a sequence of overlapping chunks according to config.
     *
     * @param text   the full document text
     * @param config chunking parameters (size and overlap in words)
     * @return ordered list of chunks; empty if text is blank
     */
    List<DocumentChunk> chunk(String text, ChunkConfig config);
}
