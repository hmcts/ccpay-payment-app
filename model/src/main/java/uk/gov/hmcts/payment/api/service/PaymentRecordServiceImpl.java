package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.Arrays;
import java.util.List;

@Service
public class PaymentRecordServiceImpl implements PaymentRecordService<PaymentFeeLink, String> {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentRecordServiceImpl.class);

    private final static String PAYMENT_CHANNEL_DIGITAL_BAR = "digital bar";
    private final static String PAYMENT_STATUS_CREATED = "created";

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentReferenceUtil paymentReferenceUtil;
    private final UserIdSupplier userIdSupplier;

    @Autowired
    public PaymentRecordServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                    PaymentChannelRepository paymentChannelRepository,
                                    PaymentMethodRepository paymentMethodRepository,
                                    PaymentStatusRepository paymentStatusRepository,
                                    PaymentReferenceUtil paymentReferenceUtil,
                                    UserIdSupplier userIdSupplier) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentReferenceUtil = paymentReferenceUtil;
        this.userIdSupplier = userIdSupplier;
    }


    @Override
    public PaymentFeeLink recordPayment(Payment recordPayment, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException {
        LOG.debug("Record payment with PaymentGroupReference: {}", paymentGroupReference);

        PaymentFeeLink paymentFeeLink = populatePaymentDetails(recordPayment, fees, paymentGroupReference);

        return  paymentFeeLinkRepository.save(paymentFeeLink);
    }

    protected PaymentFeeLink populatePaymentDetails(Payment payment, List<PaymentFee> fees, String paymentGroupRef) throws CheckDigitException {

        return PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupRef)
            .payments(Arrays.asList(Payment.paymentWith()
                .amount(payment.getAmount())
                .caseReference(payment.getCaseReference())
                .currency(payment.getCurrency())
                .siteId(payment.getSiteId())
                .externalProvider(payment.getExternalProvider())
                .externalReference(payment.getExternalReference())
                .giroSlipNo(payment.getGiroSlipNo())
                .serviceType(payment.getServiceType())
                .userId(userIdSupplier.get())
                .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_DIGITAL_BAR))
                .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED))
                .paymentMethod(paymentMethodRepository.findByNameOrThrow(payment.getPaymentMethod().getName()))
                .reference(paymentReferenceUtil.getNext())
                .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                    .status(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED).getName())
                    .build()))
                .build()))
            .fees(fees)
            .build();
    }
}
