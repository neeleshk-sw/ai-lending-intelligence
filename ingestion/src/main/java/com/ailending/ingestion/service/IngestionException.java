package com.ailending.ingestion.service;

import com.ailending.common.exception.BaseException;

public class IngestionException extends BaseException {
    public IngestionException(String message) {
        super(message);
    }
}
