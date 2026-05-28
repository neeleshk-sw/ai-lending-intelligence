package com.ailending.integration.notification;

import com.ailending.integration.exception.IntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryNotificationAdapterTest {

    private InMemoryNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryNotificationAdapter();
    }

    @Test
    void send_storesNotification_retrievableViaSent() {
        NotificationRequest request = new NotificationRequest(
                "cust-1", NotificationChannel.EMAIL,
                "Loan Submitted", "Your loan application has been received.");

        adapter.send(request);

        assertEquals(1, adapter.getSent().size());
        assertSame(request, adapter.getSent().get(0));
    }

    @Test
    void send_emailAndSms_bothAccepted() {
        adapter.send(new NotificationRequest("cust-1", NotificationChannel.EMAIL, "subj", "body"));
        adapter.send(new NotificationRequest("cust-1", NotificationChannel.SMS,   "subj", "body"));

        assertEquals(2, adapter.getSent().size());
        assertEquals(NotificationChannel.EMAIL, adapter.getSent().get(0).getChannel());
        assertEquals(NotificationChannel.SMS,   adapter.getSent().get(1).getChannel());
    }

    @Test
    void send_nullRequest_throwsIntegrationException() {
        assertThrows(IntegrationException.class, () -> adapter.send(null));
    }

    @Test
    void clear_emptiesStoredNotifications() {
        adapter.send(new NotificationRequest("cust-1", NotificationChannel.SMS, "s", "b"));
        adapter.clear();

        assertTrue(adapter.getSent().isEmpty());
    }

    @Test
    void multipleRecipients_allStoredSeparately() {
        adapter.send(new NotificationRequest("cust-A", NotificationChannel.EMAIL, "s1", "b1"));
        adapter.send(new NotificationRequest("cust-B", NotificationChannel.EMAIL, "s2", "b2"));
        adapter.send(new NotificationRequest("cust-C", NotificationChannel.SMS,   "s3", "b3"));

        assertEquals(3, adapter.getSent().size());
    }
}
