package com.ailending.aicore.prompt;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PromptTemplate rendering and PromptBuilder assembly logic.
 */
public class PromptBuilderTest {

    @Test
    public void testRenderTemplate() {
        PromptTemplate template = new PromptTemplate("test", "Hello {name}, your score is {score}.");
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "Neelesh");
        vars.put("score", 95);

        String result = template.render(vars);
        assertEquals("Hello Neelesh, your score is 95.", result);
    }

    @Test
    public void testBuildPrompt() {
        PromptTemplate template = new PromptTemplate("rag-test", "Context:\n{context}\n\nQuestion: {query}");
        PromptContext context = new PromptContext();
        context.set("query", "Is the income verified?");

        PromptBuilder builder = new PromptBuilder();
        String result = builder.buildPrompt(template, context, "The income is fully verified via pay stubs.");

        assertEquals("Context:\nThe income is fully verified via pay stubs.\n\nQuestion: Is the income verified?", result);
    }
}
