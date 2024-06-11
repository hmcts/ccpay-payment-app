package uk.gov.hmcts.payment.api.service.govpay;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;

import java.util.List;


@Service
public class GovPayDelegatingPaymentService implements DelegatingPaymentService<GovPayPayment, String> {
    private static final Logger LOG = LoggerFactory.getLogger(GovPayDelegatingPaymentService.class);
    private final GovPayKeyRepository govPayKeyRepository;
    private final GovPayClient govPayClient;
    private final ServiceIdSupplier serviceIdSupplier;
    private final GovPayAuthUtil govPayAuthUtil;

    private final ServiceToTokenMap serviceToTokenMap;

    @Autowired
    public GovPayDelegatingPaymentService(GovPayKeyRepository govPayKeyRepository, GovPayClient govPayClient, ServiceIdSupplier serviceIdSupplier, GovPayAuthUtil govPayAuthUtil,
    ServiceToTokenMap serviceToTokenMap) {
        this.govPayKeyRepository = govPayKeyRepository;
        this.govPayClient = govPayClient;
        this.serviceIdSupplier = serviceIdSupplier;
        this.govPayAuthUtil = govPayAuthUtil;
        this.serviceToTokenMap = serviceToTokenMap;
    }

    @Override
    public GovPayPayment create(PaymentServiceRequest paymentServiceRequest) {
        String key = keyForService();
        LOG.info("Language value in GovPayDelegatingPaymentService: {}", paymentServiceRequest.getLanguage());
        return govPayClient.createPayment(key, new CreatePaymentRequest(paymentServiceRequest.getAmount().movePointRight(2).intValue(),
            paymentServiceRequest.getPaymentReference(), paymentServiceRequest.getDescription(),
            paymentServiceRequest.getReturnUrl(), paymentServiceRequest.getLanguage()));
    }

    @Override
    public GovPayPayment create(CreatePaymentRequest createPaymentRequest, String serviceName) {
        LOG.info("Gov Pay Delegating service --- createPaymentRequest.getReturnUrl() {}",createPaymentRequest.getReturnUrl());
        LOG.info("Gov Pay Delegating service ---"+serviceName);
        String key = getServiceKeyWithServiceName(serviceName);
        LOG.info("Key value: {}",key);
        LOG.info("Language value in GovPayDelegatingPaymentService - CreatePaymentRequest: {}", createPaymentRequest.getLanguage());
        return govPayClient.createPayment(key, createPaymentRequest);
    }

    @Override
    public void cancel(Payment payment, String ccdCaseNumber) {
        // Do nothing
    }

    @Override
    public void cancel(Payment payment, String ccdCaseNumber, String serviceName) {
        // Do nothing
    }

    @Override
    public GovPayPayment update(PaymentServiceRequest paymentServiceRequest) {
        return null;
    }

    @Override
    public GovPayPayment retrieve(@NonNull String id) {
        return govPayClient.retrievePayment(keyForService(), id);
    }

    @Override
    public GovPayPayment retrieve(PaymentFeeLink paymentFeeLink, String paymentReference) {
        return null;
    }

    @Override
    public GovPayPayment retrieve(@NonNull String id, @NonNull String service) {
        return govPayClient.retrievePayment(keyForService(service), id);
    }

    @Override
    public List<GovPayPayment> search(PaymentSearchCriteria searchCriteria) {
        return null;
    }

    @Override
    public void cancel(String cancelUrl) {
        govPayClient.cancelPayment(keyForService(), cancelUrl);
    }

    @Override
    public void cancel(String cancelUrl, String serviceName) {
        LOG.info("NEW cancel in gov pay delegating service");
        govPayClient.cancelPayment(getServiceKeyWithServiceName(serviceName), cancelUrl);
    }

    @Override
    public List<Payment> searchByCriteria(PaymentSearchCriteria searchCriteria) {
        return null;
    }

    private String hrefFor(Link link) {
        if (link == null) {
            throw new UnsupportedOperationException("Requested action is not available for the payment");
        }

        return link.getHref();
    }

    private String keyForService(String service) {
        LOG.info("KEY FOR SERVICE: "+service);
        return govPayAuthUtil.getServiceToken(service);
    }

    private String keyForService() {
        return govPayKeyRepository.getKey(serviceIdSupplier.get());
    }

    private String getServiceKeyWithServiceName(String serviceName) {
        LOG.info("service name {}",serviceName);
        LOG.info("servicesMap {}", serviceToTokenMap.getServiceKeyVaultName(serviceName));
        return govPayKeyRepository.getKey(serviceToTokenMap.getServiceKeyVaultName(serviceName));
    }
}
