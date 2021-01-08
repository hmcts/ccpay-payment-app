package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import java.util.Optional;

@Service
public class CardDetailsServiceImpl implements CardDetailsService<CardDetails, String> {
    private static final Logger LOG = LoggerFactory.getLogger(CardDetailsServiceImpl.class);

    private final static String PAYMENT_BY_CARD = "card";

    private final DelegatingPaymentService<GovPayPayment, String> delegate;

    private final Payment2Repository paymentRespository;
    private final GovPayAuthUtil govPayAuthUtil;
    private final ServiceIdSupplier serviceIdSupplier;

    @Autowired
    public CardDetailsServiceImpl(DelegatingPaymentService<GovPayPayment, String> delegate, Payment2Repository paymentRespository, GovPayAuthUtil govPayAuthUtil, ServiceIdSupplier serviceIdSupplier) {
        this.delegate = delegate;
        this.paymentRespository = paymentRespository;
        this.govPayAuthUtil = govPayAuthUtil;
        this.serviceIdSupplier = serviceIdSupplier;
    }


    @Override
    public CardDetails retrieve(String paymentReference) {
        Payment payment = paymentRespository.findByReferenceAndPaymentMethod(paymentReference,
            PaymentMethod.paymentMethodWith().name(PAYMENT_BY_CARD).build()).orElseThrow(PaymentNotFoundException::new);

        String paymentService = govPayAuthUtil.getServiceName(serviceIdSupplier.get(), payment.getS2sServiceName());
        LOG.error("Payment Service passed to Gov pay: {}", paymentService);
        GovPayPayment govPayPayment = delegate.retrieve(payment.getExternalReference(), paymentService);

        Optional<CardDetails> opt = Optional.ofNullable(govPayPayment.getCardDetails());
        CardDetails cardDetails = null;
        if (opt.isPresent()) {
            LOG.debug("Payment card details found for the reference: {}", payment.getReference());
            cardDetails = opt.get();
            cardDetails.setEmail(govPayPayment.getEmail());
        } else {
            LOG.error("Payment card details not found for the reference: {}", payment.getReference());
            throw new PaymentNotFoundException("Payment card details not found.");
        }

        return cardDetails;
    }
}
