package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.RetroRemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.StatusResultMatchersExtensionsKt.isEqualTo;

public class RemissionServiceTest {

    @InjectMocks
    private RemissionServiceImpl remissionService;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private RemissionRepository remissionRepository;

    @Spy
    private ReferenceUtil referenceUtil;

    @Mock
    private ServiceRequestCaseUtil serviceRequestCaseUtil;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createRemissionTest() throws Exception {
        Remission remission = getRemission();
        PaymentFee fee = getFee();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2019-123456789")
            .remissions(Collections.singletonList(remission))
            .fees(Arrays.asList(fee))
            .build();

        RemissionServiceRequest remissionServiceRequest = RemissionServiceRequest.remissionServiceRequestWith()
            .paymentGroupReference("2019-123456789")
            .ccdCaseNumber("1111-2222-3333-4444")
            .beneficiaryName("testCreateRemission")
            .hwfAmount(new BigDecimal("100.99"))
            .hwfReference("hwf123456789")
            .fee(fee)
            .build();

        when(serviceRequestCaseUtil.enhanceWithServiceRequestCaseDetails(any(PaymentFeeLink.class), any(RemissionServiceRequest.class))).thenReturn(paymentFeeLink);
        when(paymentFeeLinkRepository.save(any(PaymentFeeLink.class))).thenReturn(paymentFeeLink);

        PaymentFeeLink res = remissionService.createRemission(remissionServiceRequest);

        assertThat(res).isNotNull();
        assertThat(res.getPaymentReference()).isEqualTo("2019-123456789");
        res.getRemissions().stream().forEach(r -> {
            assertThat(r.getRemissionReference()).isEqualTo("RM-1555-0684-8011-0463");
        });

        verify(paymentFeeLinkRepository, times(1)).save(any(PaymentFeeLink.class));
    }

    @Test(expected = InvalidPaymentGroupReferenceException.class)
    public void createPartialRemissionWithInvalidPaymentGroupReferenceTest() throws Exception {
        Remission remission = getRemission();
        PaymentFee fee = getFee();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2019-123456799")
            .remissions(Collections.singletonList(remission))
            .fees(Arrays.asList(fee))
            .build();

        when(paymentFeeLinkRepository.save(any(PaymentFeeLink.class))).thenReturn(paymentFeeLink);

        remissionService.createRetrospectiveRemission(RemissionServiceRequest.remissionServiceRequestWith().build(), "2019-000000099", 1);
    }

    @Test
    public void createPartialRemissionWithValidPaymentGroupReferenceTest() throws Exception {
        Remission remission = getRemission();
        PaymentFee fee = getFee();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2019-123456799")
            .remissions(Collections.singletonList(remission))
            .fees(Arrays.asList(fee))
            .build();

        when(paymentFeeLinkRepository.findByPaymentReference("2019-123456799")).thenReturn(Optional.ofNullable(paymentFeeLink));

        RemissionServiceRequest remissionServiceRequest = RemissionServiceRequest.remissionServiceRequestWith()
            .paymentGroupReference("2019-123456799")
            .ccdCaseNumber("1111-2222-3333-4444")
            .beneficiaryName("testCreateRemission")
            .hwfAmount(new BigDecimal("100.99"))
            .hwfReference("hwf123456789")
            .fee(fee)
            .build();

        PaymentFeeLink res = remissionService.createRetrospectiveRemission(remissionServiceRequest, paymentFeeLink.getPaymentReference(), 1);
        assertThat(res).isNotNull();
        assertThat(res.getPaymentReference()).isEqualTo(paymentFeeLink.getPaymentReference());

        verify(paymentFeeLinkRepository, atLeastOnce()).findByPaymentReference(paymentFeeLink.getPaymentReference());
    }

    @Test
    public void createPartialRemissionWithPaymentGroupReferenceAndCcdCaseNumberTest() throws Exception {
        Remission remission = getRemission();
        PaymentFee fee = getFee();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2019-123456799")
            .remissions(Collections.singletonList(remission))
            .ccdCaseNumber("1111-2222-3333-4444")
            .fees(Arrays.asList(fee))
            .build();

        when(paymentFeeLinkRepository.findByPaymentReferenceAndCcdCaseNumber("2019-123456799", "1111-2222-3333-4444")).thenReturn(Optional.ofNullable(paymentFeeLink));

        RemissionServiceRequest remissionServiceRequest = RemissionServiceRequest.remissionServiceRequestWith()
            .paymentGroupReference("2019-123456799")
            .ccdCaseNumber("1111-2222-3333-4444")
            .beneficiaryName("testCreateRemission")
            .hwfAmount(new BigDecimal("100.99"))
            .hwfReference("hwf123456789")
            .fee(fee)
            .build();

        PaymentFeeLink res = remissionService.createRetrospectiveRemission(remissionServiceRequest, paymentFeeLink.getPaymentReference(), 1);
        assertThat(res).isNotNull();
        assertThat(res.getPaymentReference()).isEqualTo(paymentFeeLink.getPaymentReference());

        verify(paymentFeeLinkRepository, atLeastOnce()).findByPaymentReference(paymentFeeLink.getPaymentReference());
        verify(paymentFeeLinkRepository, atLeastOnce()).findByPaymentReferenceAndCcdCaseNumber(paymentFeeLink.getPaymentReference(), paymentFeeLink.getCcdCaseNumber());
    }

    @Test
    public void createPartialRetroRemissionWithPaymentGroupReferenceAndFeeIdTest() throws Exception {
        PaymentFee fee = getFee();
        Remission remission = getRemission();
        List<Remission> emptyRemissions = new ArrayList<>();
        fee.setRemissions(Collections.singletonList(remission));

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference("2019-123456799")
            .remissions(Collections.singletonList(remission))
            .fees(Arrays.asList(fee))
            .build();

        fee.setRemissions(emptyRemissions);
        when(paymentFeeLinkRepository.findByPaymentReferenceAndFeeId("2019-123456799", 1)).thenReturn(Optional.ofNullable(paymentFeeLink));

        RetroRemissionServiceRequest remissionServiceRequest = RetroRemissionServiceRequest.retroRemissionServiceRequestWith()
            .hwfAmount(new BigDecimal("100.99"))
            .hwfReference("hwf123456789")
            .build();

        Remission rem = remissionService.createRetrospectiveRemissionForPayment(remissionServiceRequest, "2019-123456799",1);
        assertThat(rem).isNotNull();
        assertThat(paymentFeeLink.getPaymentReference()).isNotNull();
        assertThat(rem.getHwfAmount()).isEqualTo(remissionServiceRequest.getHwfAmount());
        assertThat(rem.getCcdCaseNumber()).isEqualTo(fee.getCcdCaseNumber());

        verify(paymentFeeLinkRepository, atLeastOnce()).findByPaymentReference(paymentFeeLink.getPaymentReference());
        verify(paymentFeeLinkRepository, atLeastOnce()).findByPaymentReferenceAndFeeId(paymentFeeLink.getPaymentReference(), fee.getId());
    }

    private Remission getRemission() {
        return Remission.remissionWith()
            .hwfAmount(new BigDecimal("100.99"))
            .remissionReference("RM-1555-0684-8011-0463")
            .hwfReference("hwf123456789")
            .ccdCaseNumber("1111-2222-3333-4444")
            .beneficiaryName("testCreateRemission")
            .build();
    }

    private PaymentFee getFee() {
        return PaymentFee.feeWith()
            .id(1)
            .ccdCaseNumber("1111-2222-3333-4444")
            .code("FEE0123")
            .version("1")
            .calculatedAmount(new BigDecimal("100.99"))
            .build();
    }
}
