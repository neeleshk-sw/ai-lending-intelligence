package com.ailending.documentintelligence.service;

import com.ailending.documentintelligence.model.DocumentLayout;

public interface DocumentIntelligenceService {

    DocumentLayout analyze(String parsedText, String documentType);
}
