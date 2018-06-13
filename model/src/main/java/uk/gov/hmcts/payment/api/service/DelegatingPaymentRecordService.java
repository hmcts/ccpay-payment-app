package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;

import java.util.Arrays;
import java.util.List;

@Service
public class DelegatingPaymentRecordService implements PaymentRecordService<PaymentFeeLink, String> {

    private static final Logger LOG = LoggerFactory.getLogger(DelegatingPaymentRecordService.class);

    private final static String PAYMENT_CHANNEL_DIGITAL_BAR = "digital bar";
    private final static String PAYMENT_METHOD_CASH = "cash";
    private final static String PAYMENT_STATUS_SUCCESS = "success";

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final Payment2Repository paymentRespository;
    private final PaymentReferenceUtil paymentReferenceUtil;

    @Autowired
    public DelegatingPaymentRecordService(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                          PaymentChannelRepository paymentChannelRepository,
                                          PaymentMethodRepository paymentMethodRepository, PaymentProviderRepository paymentProviderRepository,
                                          PaymentStatusRepository paymentStatusRepository, Payment2Repository paymentRespository,
                                          PaymentReferenceUtil paymentReferenceUtil) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentRespository = paymentRespository;
        this.paymentReferenceUtil = paymentReferenceUtil;
    }


    @Override
    public PaymentFeeLink recordPayment(Payment recordPayment, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException {
        LOG.debug("Record payment with PaymentGroupReference: {}", paymentGroupReference);


        Payment payment = null;
        try {
            payment = Payment.paymentWith()
                .amount(recordPayment.getAmount())
                .ccdCaseNumber(recordPayment.getCcdCaseNumber())
                .caseReference(recordPayment.getCaseReference())
                .currency(recordPayment.getCurrency())
                .siteId(recordPayment.getSiteId())
                .giro(recordPayment.getGiro())
                .serviceType(recordPayment.getServiceType())
                .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_DIGITAL_BAR))
                .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_SUCCESS))
                .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_CASH))
                .reference(paymentReferenceUtil.getNext())
                .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                    .status(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_SUCCESS).getName())
                    .build()))
                .build();
        } catch (CheckDigitException e) {
            LOG.error("Error in generating check digit for the payment reference, {}", e);
        }

        PaymentFeeLink result =  paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupReference)
            .payments(Arrays.asList(payment))
            .fees(fees)
            .build());

        return result;
    }
}
