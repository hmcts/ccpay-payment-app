package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class CardDetailsServiceTest {

    @Mock
    Payment2Repository paymentRespository;

    @Mock
    GovPayAuthUtil govPayAuthUtil;

    @Mock
    DelegatingPaymentService<GovPayPayment, String> delegate;

    @Mock
    ServiceIdSupplier serviceIdSupplier;

    @InjectMocks
    CardDetailsServiceImpl cardDetailsServiceImp;

    private final static String PAYMENT_BY_CARD = "card";

    @Test
    public void testRetrieve(){
//        Payment payment = Payment.paymentWith()
//                            .build();
//        when(paymentRespository.findByReferenceAndPaymentMethod(any(String.class),any(PaymentMethod.class))).thenReturn(java.util.Optional.ofNullable(payment));
//        when(govPayAuthUtil.getServiceName(any(String.class), any(String.class))).thenReturn("payment-service");
//        CardDetails cardDetails = CardDetails.cardDetailsWith()
//                                    .cardholderName("card-name")
//                                    .lastDigitsCardNumber("1234")
//                                    .build();
//        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
//                                        .amount(100)
//                                        .cardDetails(cardDetails)
//                                        .build();
//        when(delegate.retrieve(any(String.class), any(String.class))).thenReturn(govPayPayment);
//        CardDetails resultCardDetails = cardDetailsServiceImp.retrieve("payment-reference");
//        assertEquals("card-name",resultCardDetails.getCardholderName());

    }
}
