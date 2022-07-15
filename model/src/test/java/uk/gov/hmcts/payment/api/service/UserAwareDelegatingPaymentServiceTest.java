package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.service.govpay.ServiceToTokenMap;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class UserAwareDelegatingPaymentServiceTest {

    private UserIdSupplier userIdSupplier = mock(UserIdSupplier.class);
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);
    private DelegatingPaymentService<GovPayPayment, String> delegateGovPay = mock(DelegatingPaymentService.class);
    private DelegatingPaymentService<PciPalPayment, String> delegatePciPal = mock(DelegatingPaymentService.class);
    private PaymentChannelRepository paymentChannelRepository = mock(PaymentChannelRepository.class);
    private PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
    private PaymentProviderRepository paymentProviderRepository = mock(PaymentProviderRepository.class);
    private PaymentStatusRepository paymentStatusRepository = mock(PaymentStatusRepository.class);
    private Payment2Repository paymentRespository = mock(Payment2Repository.class);
    private ReferenceUtil referenceUtil = mock(ReferenceUtil.class);
    private GovPayAuthUtil govPayAuthUtil = mock(GovPayAuthUtil.class);
    private ServiceIdSupplier serviceIdSupplier = mock(ServiceIdSupplier.class);
    private AuditRepository auditRepository = mock(AuditRepository.class);
    private CallbackService callbackService = mock(CallbackService.class);

    private FeePayApportionRepository feePayApportionRepository = mock(FeePayApportionRepository.class);
    private PaymentFeeRepository paymentFeeRepository = mock(PaymentFeeRepository.class);
    private FeePayApportionService feePayApportionService = mock(FeePayApportionService.class);
    private LaunchDarklyFeatureToggler featureToggler = mock(LaunchDarklyFeatureToggler.class);
    private ServiceRequestCaseUtil serviceRequestCaseUtil = mock(ServiceRequestCaseUtil.class);
    @Mock
    private ServiceToTokenMap serviceToTokenMap;

    @InjectMocks
    private UserAwareDelegatingPaymentService userAwareDelegatingPaymentService;

    private final static String PAYMENT_CHANNEL_TELEPHONY = "telephony";
    private final static String PAYMENT_PROVIDER_PCI_PAL = "pci pal";

    @Before
    public void before() {
        userAwareDelegatingPaymentService = new UserAwareDelegatingPaymentService(userIdSupplier,
            paymentFeeLinkRepository, delegateGovPay, delegatePciPal, paymentChannelRepository, paymentMethodRepository,
            paymentProviderRepository, paymentStatusRepository, paymentRespository, referenceUtil, govPayAuthUtil,
            serviceIdSupplier, auditRepository, callbackService, feePayApportionRepository, paymentFeeRepository, feePayApportionService, featureToggler, serviceRequestCaseUtil);

        MockitoAnnotations.initMocks(this);
    }

    //calls PCI_PAL service when telephony and pci pal
    @Test
    public void callsPciPalServiceCreateWhenRequest() throws CheckDigitException {
        PaymentServiceRequest paymentServiceRequest = PaymentServiceRequest.paymentServiceRequestWith()
            .provider(PAYMENT_PROVIDER_PCI_PAL)
            .channel(PAYMENT_CHANNEL_TELEPHONY)
            .build();
        PciPalPayment pciPalPaymentExpected = PciPalPayment.pciPalPaymentWith()
            .paymentId("payment-id-1234")
            .state(State.stateWith().status("success").build())
            .build();
        when(delegatePciPal.create(paymentServiceRequest)).thenReturn(pciPalPaymentExpected);
        userAwareDelegatingPaymentService.create(paymentServiceRequest);
        verify((delegatePciPal), times(1)).create(paymentServiceRequest);
    }

    @Test
    public void whenValidInput_retrieveWithCallBack_thenPaymentFeeLinkReceived() {
        Payment payment = Payment.paymentWith()
                .s2sServiceName("S2S")
                .internalReference("InternalReference")
                .externalReference("ExternalReference")
                .paymentLink(PaymentFeeLink.paymentFeeLinkWith()
                        .enterpriseServiceName("Family Private Law")
                        .callBackUrl("EEE")
                        .ccdCaseNumber("FFF")
                        .paymentReference("GGG")
                        .payments(List.of(Payment.paymentWith().build()))
                        .build())
                .statusHistories(List.of(StatusHistory.statusHistoryWith()
                        .externalStatus("created")
                        .build()))
                .build();
        when(paymentRespository.findByReference(anyString())).thenReturn(java.util.Optional.ofNullable(payment));
        when(serviceToTokenMap.getServiceKeyVaultName(anyString())).thenReturn("prl_cos_api");
        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
                .amount(111)
                .state(State.stateWith()
                        .status("Success")
                        .code("AAA")
                        .message("BBB")
                        .finished(true)
                        .build())
                .paymentId("1")
                .description("CCC")
                .returnUrl("DDD")
                .links(GovPayPayment.Links.linksWith()
                        .nextUrl(Link.linkWith().build())
                        .cancel(Link.linkWith().build())
                        .refunds(Link.linkWith().build())
                        .build())
                .build();
        when(delegateGovPay.retrieve(anyString(), anyString())).thenReturn(govPayPayment);
        PaymentFeeLink paymentFeeLink = userAwareDelegatingPaymentService.retrieveWithCallBack("");

        assertNotNull(paymentFeeLink);
        assertEquals("FFF", paymentFeeLink.getCcdCaseNumber());
        assertEquals("GGG", paymentFeeLink.getPaymentReference());
        assertEquals(1, paymentFeeLink.getPayments().size());
    }
}
