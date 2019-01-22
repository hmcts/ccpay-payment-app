package uk.gov.hmcts.payment.api.service.govpay;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;

import java.util.List;


@Service
public class GovPayDelegatingPaymentService implements DelegatingPaymentService<GovPayPayment, String> {
    private final GovPayKeyRepository govPayKeyRepository;
    private final GovPayClient govPayClient;
    private final ServiceIdSupplier serviceIdSupplier;
    private final GovPayAuthUtil govPayAuthUtil;

    @Autowired
    public GovPayDelegatingPaymentService(GovPayKeyRepository govPayKeyRepository, GovPayClient govPayClient, ServiceIdSupplier serviceIdSupplier, GovPayAuthUtil govPayAuthUtil) {
        this.govPayKeyRepository = govPayKeyRepository;
        this.govPayClient = govPayClient;
        this.serviceIdSupplier = serviceIdSupplier;
        this.govPayAuthUtil = govPayAuthUtil;
    }

    @Override
    public GovPayPayment create(PaymentServiceRequest paymentServiceRequest) {
        String key = keyForService();
        return govPayClient.createPayment(key, new CreatePaymentRequest(paymentServiceRequest.getAmount(),
            paymentServiceRequest.getPaymentReference(), paymentServiceRequest.getDescription(),
            paymentServiceRequest.getReturnUrl()));
    }

    @Override
    public GovPayPayment retrieve(@NonNull String id) {
        return govPayClient.retrievePayment(keyForService(), id);
    }

    @Override
    public GovPayPayment retrieve(@NonNull String id, @NonNull String service) {
        return govPayClient.retrievePayment(keyForService(service), id);
    }

    @Override
    public List<GovPayPayment> search(PaymentSearchCriteria searchCriteria) {
        return null;
    }

    private String hrefFor(Link link) {
        if (link == null) {
            throw new UnsupportedOperationException("Requested action is not available for the payment");
        }

        return link.getHref();
    }

    private String keyForService(String service) {
        return govPayAuthUtil.getServiceToken(service);
    }

    private String keyForService() {
        return govPayKeyRepository.getKey(serviceIdSupplier.get());
    }
}
