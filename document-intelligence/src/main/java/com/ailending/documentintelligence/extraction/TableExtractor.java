package com.ailending.documentintelligence.extraction;

import com.ailending.documentintelligence.model.ExtractedTable;

import java.util.List;

public interface TableExtractor {

    List<ExtractedTable> extract(String text);
}
