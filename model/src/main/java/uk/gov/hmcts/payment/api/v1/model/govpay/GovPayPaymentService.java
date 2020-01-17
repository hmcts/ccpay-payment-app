package uk.gov.hmcts.payment.api.v1.model.govpay;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.hmcts.payment.api.v1.model.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;

@Service
public class GovPayPaymentService implements PaymentService<GovPayPayment, String> {
    private final GovPayKeyRepository govPayKeyRepository;
    private final GovPayClient govPayClient;
    private final ServiceIdSupplier serviceIdSupplier;

    @Autowired
    public GovPayPaymentService(GovPayKeyRepository govPayKeyRepository, GovPayClient govPayClient, ServiceIdSupplier serviceIdSupplier) {
        this.govPayKeyRepository = govPayKeyRepository;
        this.govPayClient = govPayClient;
        this.serviceIdSupplier = serviceIdSupplier;
    }

    @Override
    public GovPayPayment create(int amount,
                                @NonNull String reference,
                                @NonNull String description,
                                @NonNull String returnUrl
                                 ) {
        return govPayClient.createPayment(keyForCurrentService(), new CreatePaymentRequest(amount, reference, description, returnUrl,null));
    }

    @Override
    public GovPayPayment retrieve(@NonNull String id) {
        return govPayClient.retrievePayment(keyForCurrentService(), id);
    }

    @Override
    public void cancel(@NonNull String id) {
        GovPayPayment payment = retrieve(id);
        govPayClient.cancelPayment(keyForCurrentService(), hrefFor(payment.getLinks().getCancel()));
    }

    @Override
    public void refund(@NonNull String id, int amount, int refundAmountAvailable) {
        GovPayPayment payment = retrieve(id);
        govPayClient.refundPayment(keyForCurrentService(), hrefFor(payment.getLinks().getRefunds()), new RefundPaymentRequest(amount, refundAmountAvailable));
    }

    private String hrefFor(Link link) {
        if (link == null) {
            throw new UnsupportedOperationException("Requested action is not available for the payment");
        }

        return link.getHref();
    }

    private String keyForCurrentService() {
        return govPayKeyRepository.getKey(serviceIdSupplier.get());
    }
}
