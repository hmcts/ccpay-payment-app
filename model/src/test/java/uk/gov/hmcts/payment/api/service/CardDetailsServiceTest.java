package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;

@RunWith(MockitoJUnitRunner.class)
public class CardDetailsServiceTest {

    @Mock
    Payment2Repository paymentRespository;

    @InjectMocks
    CardDetailsServiceImpl cardDetailsService;



    @Test
    public void testRetrieve() throws NoSuchFieldException {
        DelegatingPaymentService<GovPayPayment, String> delegate = mock( DelegatingPaymentService.class);
        GovPayAuthUtil govPayAuthUtil = mock(GovPayAuthUtil.class);
        ServiceIdSupplier serviceIdSupplier = mock(ServiceIdSupplier.class);
        ReflectionTestUtils.setField(cardDetailsService,"govPayAuthUtil",govPayAuthUtil);
        ReflectionTestUtils.setField(cardDetailsService,"paymentRespository",paymentRespository);
        ReflectionTestUtils.setField(cardDetailsService,"serviceIdSupplier",serviceIdSupplier);
        ReflectionTestUtils.setField(cardDetailsService,"delegate",delegate);
        Payment payment = paymentWith().amount(BigDecimal.valueOf(10000).movePointRight(2)).reference("reference").description("desc1").returnUrl("returnUrl1")
            .documentControlNumber("document-number")
            .s2sServiceName("ccd_gw")
            .ccdCaseNumber("ccdCaseNo1").caseReference("caseRef1").serviceType("cmc").currency("GBP")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .externalStatus("created")
                .status("Initiated")
                .build()))
            .build();
        when(paymentRespository.findByReferenceAndPaymentMethod(any(String.class),
            any(PaymentMethod.class))).thenReturn(java.util.Optional.ofNullable(payment));
        when(govPayAuthUtil.getServiceName(any(String.class), any(String.class))).thenReturn("ccd_gw");
        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
                                            .cardDetails(CardDetails.cardDetailsWith().lastDigitsCardNumber("1234").build())
                                            .amount(100)
                                            .build();
        when(delegate.retrieve(any(String.class), any(String.class))).thenReturn(govPayPayment);
        cardDetailsService.retrieve("reference");
    }

}
