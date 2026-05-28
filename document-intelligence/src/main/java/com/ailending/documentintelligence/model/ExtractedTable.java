package com.ailending.documentintelligence.model;

import java.util.Collections;
import java.util.List;

public final class ExtractedTable {

    private final List<String> headers;
    private final List<List<String>> rows;

    public ExtractedTable(List<String> headers, List<List<String>> rows) {
        this.headers = Collections.unmodifiableList(headers);
        this.rows    = Collections.unmodifiableList(rows);
    }

    public List<String> getHeaders()       { return headers; }
    public List<List<String>> getRows()    { return rows; }
}
