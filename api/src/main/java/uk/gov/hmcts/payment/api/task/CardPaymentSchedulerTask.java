package uk.gov.hmcts.payment.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class CardPaymentSchedulerTask {
    private static final Logger LOG = LoggerFactory.getLogger(CardPaymentSchedulerTask.class);

    @Value("${jobs.card.payments.email.scheduler.enable}")
    private boolean cardPaymentSchedulerEnabled;


    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Scheduled(cron = "${jobs.card.payments.email.schedule}")
    @Transactional(readOnly = true)
    public void execute() {

        if (cardPaymentSchedulerEnabled) {

            LOG.info("Sending email with csv attachment - {} ", dateFormat.format(new Date()));
        }

    }
}
