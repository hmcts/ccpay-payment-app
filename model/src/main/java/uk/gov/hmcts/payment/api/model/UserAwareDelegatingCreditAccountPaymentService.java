package uk.gov.hmcts.payment.api.model;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserAwareDelegatingCreditAccountPaymentService implements CreditAccountPaymentService<PaymentFeeLink, String> {

    private static final Logger LOG = LoggerFactory.getLogger(UserAwareDelegatingCreditAccountPaymentService.class);

    private final static String PAYMENT_CHANNEL_ONLINE = "online";
    private final static String PAYMENT_PROVIDER_MIDDLE_OFFICE_PROVIDER = "middle office provider";
    private final static String PAYMENT_STATUS_CREATED = "created";
    private final static String PAYMENT_METHOD_BY_ACCOUNT =  "payment by account";

    private final UserIdSupplier userIdSupplier;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PaymentProviderRepository paymentProviderRespository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final Payment2Repository paymentRespository;
    private final PaymentReferenceUtil paymentReferenceUtil;

    @Autowired
    public UserAwareDelegatingCreditAccountPaymentService(UserIdSupplier userIdSupplier, PaymentFeeLinkRepository paymentFeeLinkRepository,
                                                          PaymentChannelRepository paymentChannelRepository,
                                                 PaymentMethodRepository paymentMethodRepository, PaymentProviderRepository paymentProviderRepository,
                                                 PaymentStatusRepository paymentStatusRepository, Payment2Repository paymentRespository,
                                                 PaymentReferenceUtil paymentReferenceUtil) {
        this.userIdSupplier = userIdSupplier;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentProviderRespository = paymentProviderRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentRespository = paymentRespository;
        this.paymentReferenceUtil = paymentReferenceUtil;
    }


    @Override
    @Transactional
    public PaymentFeeLink create(List<Payment> creditAccounts, List<Fee> fees, String paymentGroupRef) throws CheckDigitException {
        LOG.debug("Create credit account payment with PaymentGroupReference: {}", paymentGroupRef);

        List<Payment> payments = creditAccounts.stream().map(p -> {

            Payment payment = null;
            try {
                payment = Payment.paymentWith().userId(userIdSupplier.get())
                    .amount(p.getAmount())
                    .description(p.getDescription())
                    .returnUrl(p.getReturnUrl())
                    .ccdCaseNumber(p.getCcdCaseNumber())
                    .caseReference(p.getCaseReference())
                    .currency(p.getCurrency())
                    .siteId(p.getSiteId())
                    .serviceType(p.getServiceType())
                    .customerReference(p.getCustomerReference())
                    .organisationName(p.getOrganisationName())
                    .pbaNumber(p.getPbaNumber())
                    .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE))
                    .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_BY_ACCOUNT))
                    .paymentProvider(paymentProviderRespository.findByNameOrThrow(PAYMENT_PROVIDER_MIDDLE_OFFICE_PROVIDER))
                    .paymentStatus(paymentStatusRepository.findByNameOrThrow(PAYMENT_STATUS_CREATED))
                    .reference(paymentReferenceUtil.getNext())
                    .build();
            } catch (CheckDigitException e) {
                LOG.error("Error in generating check digit for the payment reference, {}", e);
            }

            return payment;
        }).collect(Collectors.toList());


        return paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupRef)
            .payments(payments)
            .fees(fees)
            .build());
    }

    @Override
    public PaymentFeeLink retrieveByPaymentGroupReference(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference).orElseThrow(PaymentNotFoundException::new);
    }

    @Override
    public PaymentFeeLink retrieveByPaymentReference(String paymentReference) {
        Payment payment = findSavedPayment(paymentReference);
        return payment.getPaymentLink();
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRespository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }
}
