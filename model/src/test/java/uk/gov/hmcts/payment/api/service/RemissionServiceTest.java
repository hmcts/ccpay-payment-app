package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RemissionServiceTest {

    @InjectMocks
    private RemissionServiceImpl remissionService;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private RemissionRepository remissionRepository;

    @Spy
    private ReferenceUtil referenceUtil;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createRemissionTest() throws Exception {
        Remission remission = Remission.remissionWith()
            .hwfAmount(new BigDecimal("100.99"))
            .remissionReference("RM-1555-0684-8011-0463")
            .hwfReference("hwf123456789")
            .ccdCaseNumber("1111-2222-3333-4444")
            .beneficiaryName("testCreateRemission")
            .build();

        PaymentFee fee = PaymentFee.feeWith()
            .ccdCaseNumber("1111-2222-3333-4444")
            .code("FEE0123")
            .version("1")
            .calculatedAmount(new BigDecimal("100.99"))
            .build();

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

        when(paymentFeeLinkRepository.save(any(PaymentFeeLink.class))).thenReturn(paymentFeeLink);

        PaymentFeeLink res = remissionService.createRemission(remissionServiceRequest);

        assertThat(res).isNotNull();
        assertThat(res.getPaymentReference()).isEqualTo("2019-123456789");
        res.getRemissions().stream().forEach(r -> {
            assertThat(r.getRemissionReference()).isEqualTo("RM-1555-0684-8011-0463");
        });

        verify(paymentFeeLinkRepository, times(1)).save(any(PaymentFeeLink.class));

    }
}
