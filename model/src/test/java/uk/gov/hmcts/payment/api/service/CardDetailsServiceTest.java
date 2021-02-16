package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

@RunWith(SpringRunner.class)
public class CardDetailsServiceTest {

    @Mock
    Payment2Repository paymentRespository;

    @Mock
    DelegatingPaymentService<GovPayPayment, String> delegate;

    @Mock
    GovPayAuthUtil govPayAuthUtil;

    @Mock
    ServiceIdSupplier serviceIdSupplier;

    @InjectMocks
    CardDetailsServiceImpl cardDetailsService;

    @Test
    public void testRetrieve(){
        Payment payment = paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference1").description("desc1").returnUrl("returnUrl1")
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .externalStatus("created")
                .status("Initiated")
                .build()))
            .build();
        when(paymentRespository.findByReferenceAndPaymentMethod(any(String.class),
            any(PaymentMethod.class))).thenReturn(java.util.Optional.ofNullable(payment));
        when(govPayAuthUtil.getServiceName(any(String.class), any(String.class))).thenReturn("cmc");
        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
                                            .build();
        when(delegate.retrieve(any(String.class), any(String.class))).thenReturn(govPayPayment);
        cardDetailsService.retrieve("reference");
    }

}
