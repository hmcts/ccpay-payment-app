package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CardDetailsServiceTest {
//    @Mock
//    Payment2Repository paymentRespository;

    @Mock
    DelegatingPaymentService<GovPayPayment, String> delegate;

    @Mock
    GovPayAuthUtil govPayAuthUtil;

    @Mock
    ServiceIdSupplier serviceIdSupplier;

    private final static String PAYMENT_BY_CARD = "card";

//    @InjectMocks
//    CardDetailsServiceImpl cardDetailsService = new CardDetailsServiceImpl(delegate,paymentRespository,govPayAuthUtil,serviceIdSupplier);

    @Test
    public void testRetrieve(){

        Payment2Repository paymentRespository = mock(Payment2Repository.class);
        Payment payment = Payment.paymentWith()
                            .build();
        CardDetailsServiceImpl cardDetailsService = new CardDetailsServiceImpl(delegate,paymentRespository,govPayAuthUtil,serviceIdSupplier);
        when(paymentRespository.findByReferenceAndPaymentMethod(any(String.class),
            PaymentMethod.paymentMethodWith().name(PAYMENT_BY_CARD).build())).thenReturn(java.util.Optional.ofNullable(payment));
        when(govPayAuthUtil.getServiceName(any(String.class), any(String.class))).thenReturn("payment-service");
        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
                                        .build();
        when(delegate.retrieve(any(String.class), any(String.class))).thenReturn(govPayPayment);
        CardDetails cardDetails = cardDetailsService.retrieve("payment-reference");

    }
}
