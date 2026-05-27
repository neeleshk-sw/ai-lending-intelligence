package com.ailending.aicore.prompt;

import java.util.Map;

/**
 * Represents a structured prompt template containing system instructions and placeholders.
 */
public class PromptTemplate {

    private final String name;
    private final String templateText;

    public PromptTemplate(String name, String templateText) {
        this.name = name;
        this.templateText = templateText;
    }

    public String getName() {
        return name;
    }

    public String getTemplateText() {
        return templateText;
    }

    /**
     * Renders the template by replacing placeholders in the form of {variableName} with values.
     *
     * @param variables the map containing placeholder replacement values
     * @return the rendered prompt string
     */
    public String render(Map<String, Object> variables) {
        String rendered = templateText;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            rendered = rendered.replace("{" + entry.getKey() + "}", value);
        }
        return rendered;
    }
}
