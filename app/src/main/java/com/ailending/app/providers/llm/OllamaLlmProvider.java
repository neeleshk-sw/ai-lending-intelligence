package com.ailending.app.providers.llm;

import com.ailending.aicore.llm.LlmException;
import com.ailending.aicore.llm.LlmProvider;
import com.ailending.aicore.llm.LlmRequest;
import com.ailending.aicore.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

/**
 * Implementation of LlmProvider using Spring AI's ChatModel (configured for Ollama).
 * Lives in the app module so that ai-core remains Spring-free.
 */
@Service
public class OllamaLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

    private final ChatModel chatModel;

    public OllamaLlmProvider(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        try {
            Prompt prompt = new Prompt(
                request.getPrompt(),
                OllamaOptions.create()
                    .withModel(request.getModel())
                    .withTemperature((float) request.getTemperature())
            );

            long startTime = System.currentTimeMillis();
            ChatResponse chatResponse = chatModel.call(prompt);
            long durationMs = System.currentTimeMillis() - startTime;

            String text = "";
            if (chatResponse != null && chatResponse.getResult() != null
                    && chatResponse.getResult().getOutput() != null) {
                text = chatResponse.getResult().getOutput().getContent();
            }

            int tokenCount = 0;
            if (chatResponse != null && chatResponse.getMetadata() != null
                    && chatResponse.getMetadata().getUsage() != null) {
                Long generationTokens = chatResponse.getMetadata().getUsage().getGenerationTokens();
                tokenCount = generationTokens != null ? generationTokens.intValue() : 0;
            }

            return new LlmResponse(text, request.getModel(), tokenCount, durationMs);
        } catch (Exception e) {
            log.error("Failed to generate response using Ollama LLM provider", e);
            throw new LlmException("LLM generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return chatModel != null;
        } catch (Exception e) {
            log.warn("Ollama LLM provider is not available", e);
            return false;
        }
    }
}
