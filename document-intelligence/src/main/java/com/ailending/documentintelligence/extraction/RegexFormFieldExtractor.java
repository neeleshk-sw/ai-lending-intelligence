package com.ailending.documentintelligence.extraction;

import com.ailending.documentintelligence.model.FormField;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexFormFieldExtractor implements FormFieldExtractor {

    // "Key: Value" — high confidence
    private static final Pattern COLON_PATTERN =
            Pattern.compile("^([\\w][\\w\\s]{0,49}):\\s+(.+)$");

    // "Key — Value" or "Key - Value" — lower confidence
    private static final Pattern DASH_PATTERN =
            Pattern.compile("^([\\w][\\w\\s]{0,49})\\s+[—\\-]\\s+(.+)$");

    @Override
    public List<FormField> extract(String text) {
        List<FormField> fields = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return fields;
        }

        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher colon = COLON_PATTERN.matcher(trimmed);
            if (colon.matches()) {
                fields.add(new FormField(colon.group(1).trim(), colon.group(2).trim(), 0.9));
                continue;
            }

            Matcher dash = DASH_PATTERN.matcher(trimmed);
            if (dash.matches()) {
                fields.add(new FormField(dash.group(1).trim(), dash.group(2).trim(), 0.7));
            }
        }

        return fields;
    }
}
