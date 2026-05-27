package com.ailending.ingestion.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OverlapChunkerTest {

    private final OverlapChunker chunker = new OverlapChunker();

    @Test
    void blankTextReturnsEmptyList() {
        assertTrue(chunker.chunk("", ChunkConfig.defaults()).isEmpty());
        assertTrue(chunker.chunk("  ", ChunkConfig.defaults()).isEmpty());
    }

    @Test
    void textShorterThanChunkSizeProducesOneChunk() {
        String text = "hello world foo bar";
        ChunkConfig config = new ChunkConfig(50, 10);
        List<DocumentChunk> chunks = chunker.chunk(text, config);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0).getText());
        assertEquals(0, chunks.get(0).getSequence());
    }

    @Test
    void chunksHaveCorrectOverlap() {
        // 10 words, chunkSize=6, overlap=2 → step=4
        // chunk0: words 0-5, chunk1: words 4-9
        String[] words = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
        String text = String.join(" ", words);
        ChunkConfig config = new ChunkConfig(6, 2);

        List<DocumentChunk> chunks = chunker.chunk(text, config);

        assertEquals(2, chunks.size());
        assertEquals("a b c d e f", chunks.get(0).getText());
        assertEquals("e f g h i j", chunks.get(1).getText());
        assertEquals(0, chunks.get(0).getSequence());
        assertEquals(1, chunks.get(1).getSequence());
    }

    @Test
    void sequenceNumbersAreContiguousFromZero() {
        String text = "w1 w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12";
        ChunkConfig config = new ChunkConfig(4, 1);
        List<DocumentChunk> chunks = chunker.chunk(text, config);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getSequence());
        }
    }

    @Test
    void chunkConfigRejectsInvalidOverlap() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkConfig(5, 5));
        assertThrows(IllegalArgumentException.class, () -> new ChunkConfig(5, -1));
    }
}
