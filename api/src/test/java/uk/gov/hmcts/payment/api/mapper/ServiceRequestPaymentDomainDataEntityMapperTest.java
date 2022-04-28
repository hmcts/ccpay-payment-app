package uk.gov.hmcts.payment.api.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentRefDataNotFoundException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestPaymentDomainDataEntityMapperTest {

    @InjectMocks
    ServiceRequestPaymentDomainDataEntityMapper serviceRequestPaymentDomainDataEntityMapper;

    @Mock
    UserIdSupplier userIdSupplier;

    @Mock
    ServiceIdSupplier serviceIdSupplier;

    @Spy
    PaymentChannelRepository paymentChannelRepository;

    @Test
    public void toEntityTest(){
        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
            .paymentReference("RC-ref")
            .accountNumber("a")
            .amount(new BigDecimal(20))
            .currency(CurrencyCode.GBP)
            .customerReference("s")
            .accountNumber("pBa")
            .organisationName("sommin")
            .status("Success")
            .customerReference("rnd")
            .paymentReference("rnd")
            .build();

        Mockito.when(userIdSupplier.get()).thenReturn("userID");

        Mockito.when(serviceIdSupplier.get()).thenReturn("s2sServiceName");

        try {
            serviceRequestPaymentDomainDataEntityMapper.toEntity(serviceRequestPaymentBo,getPaymentFeeLink());
        }catch (PaymentRefDataNotFoundException e){

        }
    }

    @Test
    public void toDomainTest(){
        Payment payment = Payment.paymentWith()
            .paymentLink(getPaymentFeeLink())
            .paymentStatus(PaymentStatus.SUCCESS)
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith().status("status").build()))
            .dateCreated(new Date())
            .build();

        serviceRequestPaymentDomainDataEntityMapper.toDomain(payment);

    }



    private PaymentFeeLink getPaymentFeeLink() {
        return PaymentFeeLink.paymentFeeLinkWith()
            .id(1)
            .orgId("org-id")
            .enterpriseServiceName("enterprise-service-name")
            .paymentReference("payment-ref")
            .caseReference("S")
            .enterpriseServiceName("S")
            .ccdCaseNumber("1607065108455502")
            .fees(Arrays.asList(PaymentFee.feeWith().
                amountDue(new BigDecimal(10)).
                calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE0001").volume(1).build()))
            .build();
    }

}
