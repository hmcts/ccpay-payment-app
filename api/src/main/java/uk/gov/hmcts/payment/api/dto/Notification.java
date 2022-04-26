package uk.gov.hmcts.payment.api.dto;

public enum Notification {

    EMAIL("EMAIL"),
    LETTER("LETTER");

    private String notification;

    Notification(String notification) {
        this.notification = notification;
    }

    public String getNotification() {
        return this.notification;
    }
}