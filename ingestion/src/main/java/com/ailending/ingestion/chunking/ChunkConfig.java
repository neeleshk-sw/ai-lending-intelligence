package com.ailending.ingestion.chunking;

/**
 * Configuration for the chunking strategy.
 * chunkSize and overlapSize are measured in words.
 */
public class ChunkConfig {

    private static final int DEFAULT_CHUNK_SIZE = 300;
    private static final int DEFAULT_OVERLAP_SIZE = 50;

    private final int chunkSize;
    private final int overlapSize;

    public ChunkConfig(int chunkSize, int overlapSize) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be positive");
        if (overlapSize < 0 || overlapSize >= chunkSize) {
            throw new IllegalArgumentException("overlapSize must be >= 0 and < chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize;
    }

    public static ChunkConfig defaults() {
        return new ChunkConfig(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public int getChunkSize() { return chunkSize; }
    public int getOverlapSize() { return overlapSize; }
}
