package com.ailending.app.controller;

import java.lang.reflect.Method;

public class InspectDocument {
    public static void inspect() {
        try {
            System.out.println("=== Document Methods ===");
            for (Method m : org.springframework.ai.document.Document.class.getDeclaredMethods()) {
                System.out.println(m.toString());
            }
            
            System.out.println("=== OllamaOptions Methods ===");
            for (Method m : org.springframework.ai.ollama.api.OllamaOptions.class.getDeclaredMethods()) {
                System.out.println(m.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
