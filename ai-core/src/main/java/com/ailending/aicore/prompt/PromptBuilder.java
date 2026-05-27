package com.ailending.aicore.prompt;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Service component responsible for combining templates, parameters, and retrieved context.
 */
@Component
public class PromptBuilder {

    /**
     * Builds a final prompt by injecting context and key variables into the specified template.
     *
     * @param template the template to compile
     * @param context the user variables context
     * @param retrievedContext the retrieved text context to inject
     * @return the final formatted prompt string
     */
    public String buildPrompt(PromptTemplate template, PromptContext context, String retrievedContext) {
        Map<String, Object> variables = new HashMap<>(context.getVariables());
        variables.put("context", retrievedContext != null ? retrievedContext : "");
        return template.render(variables);
    }
}
