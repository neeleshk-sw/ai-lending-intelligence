package com.ailending.integration.ekyc;

public interface EkycPort {

    EkycResult verify(String customerId, String aadhaarNumber);
}
