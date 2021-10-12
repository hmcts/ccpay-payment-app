package uk.gov.hmcts.payment.api.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;

import java.math.BigDecimal;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestDomainDataEntityMapperTest {

    @InjectMocks
    ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

    @Test
    public void toServiceRequestEntityTest(){

        ServiceRequestBo serviceRequestBo = ServiceRequestBo.serviceRequestBoWith()
            .callBackUrl("http://callbackUrl/")
            .fees(Collections.singletonList(getServiceRequestFee()))
            .build();

        serviceRequestDomainDataEntityMapper.toServiceRequestEntity(serviceRequestBo);
    }

    @Test
    public void toPaymentEntityTest(){

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .paymentReference("RC-ref")
            .build();

        GovPayPayment govPayPayment = GovPayPayment.govPaymentWith()
            .paymentId("id")
            .amount(100)
            .state(State.stateWith().status("created").build())
            .links(GovPayPayment.Links.linksWith().nextUrl(new Link()).build())
            .build();

        serviceRequestDomainDataEntityMapper.toPaymentEntity(serviceRequestOnlinePaymentBo,govPayPayment);
    }

    private ServiceRequestFeeBo getServiceRequestFee() {
        return ServiceRequestFeeBo.serviceRequestFeeBoWith()
            .amountDue(new BigDecimal(99.99))
            .build();
    }
}
