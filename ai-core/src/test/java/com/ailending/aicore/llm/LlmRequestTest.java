package com.ailending.aicore.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LlmRequest and its fluent Builder.
 *
 * Covers:
 *  - fluent builder sets every field correctly
 *  - default values for temperature and maxTokens
 *  - default value for model
 *  - missing / null prompt throws IllegalArgumentException
 *  - blank prompt throws IllegalArgumentException
 *  - toString() does not leak prompt content (only its length)
 */
class LlmRequestTest {

    // -----------------------------------------------------------------------
    // Happy-path: all fields set explicitly
    // -----------------------------------------------------------------------

    @Test
    void builder_setsAllFieldsCorrectly() {
        LlmRequest request = new LlmRequest.Builder()
                .prompt("Summarise the applicant's income.")
                .model("mistral:7b")
                .temperature(0.3)
                .maxTokens(1024)
                .build();

        assertEquals("Summarise the applicant's income.", request.getPrompt());
        assertEquals("mistral:7b", request.getModel());
        assertEquals(0.3, request.getTemperature(), 1e-9);
        assertEquals(1024, request.getMaxTokens());
    }

    // -----------------------------------------------------------------------
    // Defaults
    // -----------------------------------------------------------------------

    @Test
    void builder_defaultTemperatureIs0_7() {
        LlmRequest request = new LlmRequest.Builder()
                .prompt("What is the credit risk?")
                .build();

        assertEquals(0.7, request.getTemperature(), 1e-9,
                "Default temperature must be 0.7");
    }

    @Test
    void builder_defaultMaxTokensIs512() {
        LlmRequest request = new LlmRequest.Builder()
                .prompt("What is the credit risk?")
                .build();

        assertEquals(512, request.getMaxTokens(),
                "Default maxTokens must be 512");
    }

    @Test
    void builder_defaultModelIsLlama3_8b() {
        LlmRequest request = new LlmRequest.Builder()
                .prompt("What is the credit risk?")
                .build();

        assertEquals("llama3:8b", request.getModel(),
                "Default model must be llama3:8b");
    }

    // -----------------------------------------------------------------------
    // Required field: prompt
    // -----------------------------------------------------------------------

    @Test
    void build_throwsIllegalArgumentException_whenPromptIsNull() {
        LlmRequest.Builder builder = new LlmRequest.Builder();
        // prompt intentionally omitted → null

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                builder::build,
                "build() must throw IllegalArgumentException when prompt is null"
        );

        assertTrue(ex.getMessage().toLowerCase().contains("prompt"),
                "Exception message should mention 'prompt'");
    }

    @Test
    void build_throwsIllegalArgumentException_whenPromptIsBlank() {
        LlmRequest.Builder builder = new LlmRequest.Builder()
                .prompt("   "); // blank

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                builder::build,
                "build() must throw IllegalArgumentException when prompt is blank"
        );

        assertTrue(ex.getMessage().toLowerCase().contains("prompt"),
                "Exception message should mention 'prompt'");
    }

    @Test
    void build_throwsIllegalArgumentException_whenPromptIsEmptyString() {
        LlmRequest.Builder builder = new LlmRequest.Builder()
                .prompt("");

        assertThrows(IllegalArgumentException.class, builder::build,
                "build() must throw IllegalArgumentException when prompt is empty string");
    }

    // -----------------------------------------------------------------------
    // Builder returns a new instance on each build() call
    // -----------------------------------------------------------------------

    @Test
    void builder_producesIndependentInstances() {
        LlmRequest.Builder builder = new LlmRequest.Builder()
                .prompt("First prompt");

        LlmRequest first = builder.build();

        builder.prompt("Second prompt");
        LlmRequest second = builder.build();

        assertEquals("First prompt", first.getPrompt());
        assertEquals("Second prompt", second.getPrompt());
        assertNotSame(first, second);
    }

    // -----------------------------------------------------------------------
    // toString() must not expose full prompt text (only its length)
    // -----------------------------------------------------------------------

    @Test
    void toString_doesNotContainFullPromptText() {
        String sensitivePrompt = "SECRET_CUSTOMER_DATA income is 100000";
        LlmRequest request = new LlmRequest.Builder()
                .prompt(sensitivePrompt)
                .build();

        String str = request.toString();

        assertFalse(str.contains("SECRET_CUSTOMER_DATA"),
                "toString() must not leak full prompt content");
        assertTrue(str.contains(String.valueOf(sensitivePrompt.length())),
                "toString() should include prompt length for diagnostics");
    }
}
