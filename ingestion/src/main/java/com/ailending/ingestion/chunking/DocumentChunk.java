package com.ailending.ingestion.chunking;

/**
 * Represents a single text chunk produced by a Chunker.
 */
public class DocumentChunk {

    private final String text;
    private final int sequence;

    public DocumentChunk(String text, int sequence) {
        this.text = text;
        this.sequence = sequence;
    }

    public String getText() { return text; }
    public int getSequence() { return sequence; }
}
