package uk.gov.hmcts.payment.api.service.govpay;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;
import uk.gov.hmcts.payment.api.model.Fee;

import java.util.Date;
import java.util.List;


@Service
public class GovPayCardPaymentService implements CardPaymentService<GovPayPayment, String> {
    private final GovPayKeyRepository govPayKeyRepository;
    private final GovPayClient govPayClient;
    private final ServiceIdSupplier serviceIdSupplier;

    @Autowired
    public GovPayCardPaymentService(GovPayKeyRepository govPayKeyRepository, GovPayClient govPayClient, ServiceIdSupplier serviceIdSupplier) {
        this.govPayKeyRepository = govPayKeyRepository;
        this.govPayClient = govPayClient;
        this.serviceIdSupplier = serviceIdSupplier;
    }

    @Override
    public GovPayPayment create(int amount,
                                String reference,
                                @NonNull String description,
                                @NonNull String returnUrl,
                                String ccdCaseNumber,
                                String caseReference,
                                String currency,
                                String siteId,
                                String serviceType,
                                List<Fee> fees) {
        String key = keyForCurrentService();
        return govPayClient.createPayment(key, new CreatePaymentRequest(amount, reference, description, returnUrl));
    }

    @Override
    public GovPayPayment retrieve(@NonNull String id) {
        return govPayClient.retrievePayment(keyForCurrentService(), id);
    }

    @Override
    public List<GovPayPayment> search(Date startDate, Date endDate) {
        return null;
    }

    private String hrefFor(Link link) {
        if (link == null) {
            throw new UnsupportedOperationException("Requested action is not available for the payment");
        }

        return link.getHref();
    }

    private String keyForCurrentService() {

        String key = govPayKeyRepository.getKey(serviceIdSupplier.get());
        return key;
    }
}
