package uk.gov.hmcts.payment.api.email;

import java.util.List;

public class Email {

    protected String from;
    protected String to;
    protected String subject;
    protected String message;

    private List<EmailAttachment> attachments;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<EmailAttachment> attachments) {
        this.attachments = attachments;
    }

    public boolean hasAttachments() {
        return this.attachments != null && !this.attachments.isEmpty();
    }
}



