package com.ailending.integration.notification;

public final class NotificationRequest {

    private final String recipientId;
    private final NotificationChannel channel;
    private final String subject;
    private final String body;

    public NotificationRequest(String recipientId, NotificationChannel channel,
                                String subject, String body) {
        this.recipientId = recipientId;
        this.channel     = channel;
        this.subject     = subject;
        this.body        = body;
    }

    public String getRecipientId()         { return recipientId; }
    public NotificationChannel getChannel(){ return channel; }
    public String getSubject()             { return subject; }
    public String getBody()                { return body; }
}
