package uk.gov.hmcts.payment.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;

@Component
public class RefundEligibilityUtil {

    @Value("${card.lag.time}")
    private Integer cardLagTime;
    @Value("${cash.lag.time}")
    private Integer cashLagTime;
    @Value("${postalorders.lag.time}")
    private Integer postalOrderLagTime;
    @Value("${cheques.lag.time}")
    private Integer chequesLagTime;
    @Value("${pba.lag.time}")
    private Integer pbaLagTime;

    public boolean getRefundEligiblityStatus(Payment payment, long timeDuration) {

        String paymentMethod = payment.getPaymentMethod().getName();

        boolean isLagEligible = false;
        switch (paymentMethod) {
            case "card":
                isLagEligible = isLagTimeEligibile(cardLagTime, timeDuration);

                break;
            case "cash":
                isLagEligible = isLagTimeEligibile(cashLagTime, timeDuration);

                break;
            case "cheque":
                isLagEligible = isLagTimeEligibile(chequesLagTime, timeDuration);

                break;
            case "postal order":

                isLagEligible = isLagTimeEligibile(postalOrderLagTime, timeDuration);
                break;
            case "payment by account":

                isLagEligible = isLagTimeEligibile(pbaLagTime, timeDuration);
                break;
        }
        return isLagEligible;
    }

    private boolean isLagTimeEligibile(int lagTime, long timeDuration) {

        return timeDuration >= lagTime;
    }
}


