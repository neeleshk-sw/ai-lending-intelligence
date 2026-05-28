package com.ailending.documentintelligence.extraction;

import com.ailending.documentintelligence.model.ExtractedTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PipeTableExtractor implements TableExtractor {

    @Override
    public List<ExtractedTable> extract(String text) {
        List<ExtractedTable> tables = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tables;
        }

        String[] lines = text.split("\\r?\\n");
        int i = 0;
        while (i < lines.length) {
            if (isPipeRow(lines[i])) {
                // Expect: header row, separator row, data rows
                List<String> headers = parseCells(lines[i]);
                i++;

                // Skip separator row (---|---| pattern)
                if (i < lines.length && isSeparatorRow(lines[i])) {
                    i++;
                }

                List<List<String>> rows = new ArrayList<>();
                while (i < lines.length && isPipeRow(lines[i])) {
                    List<String> cells = parseCells(lines[i]);
                    if (cells.size() == headers.size()) {
                        rows.add(cells);
                    }
                    // rows with wrong column count are silently skipped
                    i++;
                }

                if (!headers.isEmpty()) {
                    tables.add(new ExtractedTable(headers, rows));
                }
            } else {
                i++;
            }
        }

        return tables;
    }

    private boolean isPipeRow(String line) {
        return line != null && line.trim().contains("|");
    }

    private boolean isSeparatorRow(String line) {
        return line != null && line.trim().replaceAll("[|\\-\\s]", "").isEmpty();
    }

    private List<String> parseCells(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("|"))   trimmed = trimmed.substring(0, trimmed.length() - 1);

        // Keep empty cells — they count toward the column total for mismatch detection
        return Arrays.stream(trimmed.split("\\|"))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
