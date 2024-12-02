package uk.gov.hmcts.payment.api.email;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailService(final JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Retryable(value = EmailFailedException.class,
        backoff = @Backoff(delay = 100, maxDelay = 500))
    public void sendEmail(final Email email) {
        try {
            final MimeMessage message = javaMailSender.createMimeMessage();
            final MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, true);

            mimeMessageHelper.setFrom(email.getFrom());
            mimeMessageHelper.setTo(email.getTo());
            mimeMessageHelper.setSubject(email.getSubject());
            mimeMessageHelper.setText(email.getMessage(),true);

            if (email.hasAttachments()) {
                for (EmailAttachment emailAttachment : email.getAttachments()) {
                    mimeMessageHelper.addAttachment(emailAttachment.getFilename(),
                        emailAttachment.getData(),
                        emailAttachment.getContentType());
                }
            }

            javaMailSender.send(message);
        } catch (Exception e) {
            throw new EmailFailedException(e.getMessage());
        }
    }
}

