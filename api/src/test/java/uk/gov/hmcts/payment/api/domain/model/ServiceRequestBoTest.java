package uk.gov.hmcts.payment.api.domain.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class ServiceRequestBoTest {

    @InjectMocks
    private ServiceRequestBo serviceRequestBo;

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Spy
    private ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

    @Test
    public void CreateServiceRequestWithExistingCcdCaseNumber() throws Exception {

        String serviceRequestReference = "2200-1619524583862";

        ServiceRequestBo serviceRequestBoDomain = getServiceRequestBoDomain(serviceRequestReference);

        when(paymentFeeLinkRepository.save(any())).thenReturn(getPaymentFeeLink());

        ServiceRequestResponseDto serviceRequestReferenceResult = serviceRequestBo.createServiceRequest(serviceRequestBoDomain);

        assertThat(serviceRequestReference).isEqualTo(serviceRequestReferenceResult.getServiceRequestReference());

    }

    @Test
    public void CreateServiceRequestWithNewCcdCaseNumber() throws Exception {

        String serviceRequestReference = "2200-1619524583862";

        ServiceRequestBo serviceRequestBoDomain = getServiceRequestBoDomain(serviceRequestReference);

        when(paymentFeeLinkRepository.save(any())).thenReturn(getPaymentFeeLink());

        ServiceRequestResponseDto serviceRequestReferenceResult = serviceRequestBo.createServiceRequest(serviceRequestBoDomain);

        assertThat(serviceRequestReference).isEqualTo(serviceRequestReferenceResult.getServiceRequestReference());

    }


    private ServiceRequestBo getServiceRequestBoDomain(String serviceRequestReference) {
        return ServiceRequestBo.serviceRequestBoWith()
            .enterpriseServiceName("DIVORCE")
            .orgId("AA001")
            .ccdCaseNumber("8696869686968696")
            .caseReference("rertyuilkjhcxdfgh")
            .reference(serviceRequestReference)
            .fees(Collections.singletonList(getServiceRequestFee())
                .stream()
                .map(feeDto -> toFeeDomain(feeDto, "8696869686968696")) // Will be removed after get api's work without ccd dependency
                .collect(Collectors.toList()))
            .build();
    }

    private PaymentFeeLink getPaymentFeeLink() {
        return PaymentFeeLink.paymentFeeLinkWith()
            .orgId("AA001")
            .enterpriseServiceName("DIVORCE")
            .paymentReference("2200-1619524583862")
            .caseReference("rertyuilkjhcxdfgh")
            .ccdCaseNumber("8696869686968696")
            .fees(Collections.singletonList(getFee()))
            .build();
    }


    private PaymentFee getFee() {
        return PaymentFee.feeWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }


    public ServiceRequestFeeBo toFeeDomain(ServiceRequestFeeDto serviceRequestFeeDto, String ccdCaseNumber) {
        return ServiceRequestFeeBo.serviceRequestFeeBoWith()
            .calculatedAmount(serviceRequestFeeDto.getCalculatedAmount())
            .code(serviceRequestFeeDto.getCode())
            .ccdCaseNumber(ccdCaseNumber)
            .version(serviceRequestFeeDto.getVersion())
            .volume(serviceRequestFeeDto.getVolume())
            .build();
    }

    private ServiceRequestFeeDto getServiceRequestFee() {
        return ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }
}
