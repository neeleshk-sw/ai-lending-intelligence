package com.ailending.integration.notification;

import com.ailending.integration.exception.IntegrationException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryNotificationAdapter implements NotificationPort {

    private final CopyOnWriteArrayList<NotificationRequest> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(NotificationRequest request) {
        if (request == null) {
            throw new IntegrationException("NotificationRequest must not be null");
        }
        sent.add(request);
    }

    public List<NotificationRequest> getSent() {
        return Collections.unmodifiableList(sent);
    }

    public void clear() {
        sent.clear();
    }
}
