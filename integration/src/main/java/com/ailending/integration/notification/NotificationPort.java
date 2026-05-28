package com.ailending.integration.notification;

public interface NotificationPort {

    void send(NotificationRequest request);
}
