package com.ailending.integration.crm;

public final class CustomerProfile {

    private final String customerId;
    private final String fullName;
    private final String email;
    private final String phone;

    public CustomerProfile(String customerId, String fullName, String email, String phone) {
        this.customerId = customerId;
        this.fullName   = fullName;
        this.email      = email;
        this.phone      = phone;
    }

    public String getCustomerId() { return customerId; }
    public String getFullName()   { return fullName; }
    public String getEmail()      { return email; }
    public String getPhone()      { return phone; }
}
