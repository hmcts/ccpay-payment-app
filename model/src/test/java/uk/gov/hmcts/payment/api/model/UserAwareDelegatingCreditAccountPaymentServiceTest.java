package uk.gov.hmcts.payment.api.model;

import com.mmnaseri.utils.spring.data.dsl.factory.RepositoryFactoryBuilder;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.util.PaymentReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.model.Payment.paymentWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(MockitoJUnitRunner.class)
public class UserAwareDelegatingCreditAccountPaymentServiceTest {

    private static final String USER_ID = "USER_ID";
    private PaymentReferenceUtil paymentReferenceUtil = mock(PaymentReferenceUtil.class);

    private PaymentChannelRepository paymentChannelRepository = mock(PaymentChannelRepository.class);
    private PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
    private PaymentProviderRepository paymentProviderRepository = mock(PaymentProviderRepository.class);
    private PaymentStatusRepository paymentStatusRepository = mock(PaymentStatusRepository.class);
    private Payment2Repository paymentRespository = mock(Payment2Repository.class);
    private PaymentFeeLinkRepository paymentFeeLinkRepository = mock(PaymentFeeLinkRepository.class);


    private UserAwareDelegatingCreditAccountPaymentService creditAccountPaymentService = new UserAwareDelegatingCreditAccountPaymentService(() -> USER_ID,
        paymentFeeLinkRepository, paymentChannelRepository, paymentMethodRepository, paymentProviderRepository, paymentStatusRepository,
        paymentRespository, paymentReferenceUtil);

    @Test
    public void createCreditAccountPaymentTest() throws Exception {
        List<Fee> fees = Arrays.asList(getFee(1));
        List<Payment> payments = new ArrayList<>(3);
        payments.add(getPayment(1));
        payments.add(getPayment(2));
        payments.add(getPayment(3));

        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith()
            .id(1)
            .paymentReference("2018-1234567890")
            .payments(payments)
            .fees(fees)
            .build();

        when(paymentFeeLinkRepository.save(paymentFeeLink)).thenReturn(paymentFeeLink);
        creditAccountPaymentService.create(payments, fees, "2018-1234567890");
    }

    private Payment getPayment(int number) throws CheckDigitException {
        when(paymentReferenceUtil.getNext()).thenReturn("RC-1234-1234-1234-111" + number);
        String reference = paymentReferenceUtil.getNext();

        return Payment.paymentWith()
            .id(number)
            .amount(new BigDecimal("6000.00"))
            .reference(reference)
            .description("description_" + number)
            .returnUrl("https://localhost")
            .ccdCaseNumber("ccdCaseNo_" + number)
            .caseReference("caseRef_" + number)
            .currency("GBP")
            .siteId("AA_00" + number)
            .serviceType("Probate")
            .customerReference("customerRef_" + number)
            .organisationName("organistation_" + number)
            .pbaNumber("pbaNumber_" + number)
            .build();
    }


    private Fee getFee(int number) {
        return Fee.feeWith()
            .id(number)
            .calculatedAmount(new BigDecimal("10000.00"))
            .code("X0123")
            .version("1")
            .build();

    }
}
