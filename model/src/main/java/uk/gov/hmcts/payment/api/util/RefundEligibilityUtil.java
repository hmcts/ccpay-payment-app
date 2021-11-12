package uk.gov.hmcts.payment.api.util;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;

@Component
public class RefundEligibilityUtil {

    @Value("${card.lagdays}")
    private  Integer cardLagDays;
    @Value("${cash.lagdays}")
    private  Integer cashLagDays;
    @Value("${postal_orders.lagdays}")
    private  Integer postalOrderLagDays;
    @Value("${cheques.lagdays}")
    private  Integer chequesLagDays;
    @Value("${pba.lagdays}")
    private  Integer pbaLagDays;

    public Date getRefundEligiblityStatus(Payment payment) {

        String paymentMethod = payment.getPaymentMethod().getName();
        Date refundEligibleDate = null;

        switch (paymentMethod) {
            case "card":
                 refundEligibleDate = Date.from(
                    payment.getDateUpdated().toInstant().plus(cardLagDays, ChronoUnit.DAYS));
            break;
            case "cash":
                 refundEligibleDate = Date.from(
                    payment.getDateUpdated().toInstant().plus(cashLagDays, ChronoUnit.DAYS));
            break;
            case "cheque":
                 refundEligibleDate = Date.from(
                    payment.getDateUpdated().toInstant().plus(chequesLagDays, ChronoUnit.DAYS));
            break;
            case "postal order":
                 refundEligibleDate = Date.from(
                    payment.getDateUpdated().toInstant().plus(postalOrderLagDays, ChronoUnit.DAYS));
            break;
            case "payment by account":
                 refundEligibleDate = Date.from(
                    payment.getDateUpdated().toInstant().plus(pbaLagDays, ChronoUnit.DAYS));
            break;
        }
        return refundEligibleDate;
    }
}


