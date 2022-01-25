package uk.gov.hmcts.payment.api.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "serviceRequestBoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class ServiceRequestBo {
    //-- All CRUD & Validation operations for Orders to be implemented

    private String reference;

    private String callBackUrl;

    private String ccdCaseNumber;

    private String caseReference;

    private String orgId;

    private String enterpriseServiceName;

    private List<ServiceRequestFeeBo> fees;

    private PaymentStatus status;

    private BigDecimal serviceRequestBalance;

    @Autowired
    private ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Transactional
    public ServiceRequestResponseDto createServiceRequest(ServiceRequestBo serviceRequestBo) {

        PaymentFeeLink paymentFeeLinkAliasServiceRequestEntity = serviceRequestDomainDataEntityMapper.toServiceRequestEntity(serviceRequestBo);

        PaymentFeeLink serviceRequestSavedWithFees = paymentFeeLinkRepository.save(paymentFeeLinkAliasServiceRequestEntity);

        ServiceRequestResponseDto serviceRequestResponseDto = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
                                                .serviceRequestReference(serviceRequestSavedWithFees.getPaymentReference())
                                                .build();

        return serviceRequestResponseDto;
    }

}
