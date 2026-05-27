package com.ailending.aicore.prompt;

import java.util.HashMap;
import java.util.Map;

/**
 * Context container holding dynamic variable mappings to render prompts.
 */
public class PromptContext {

    private final Map<String, Object> variables = new HashMap<>();

    public void set(String key, Object value) {
        variables.put(key, value);
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public Map<String, Object> getVariables() {
        return variables;
    }
}
