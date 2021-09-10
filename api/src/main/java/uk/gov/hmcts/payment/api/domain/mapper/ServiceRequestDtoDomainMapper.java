package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestFeeDto;

import java.util.stream.Collectors;

@Component
public class ServiceRequestDtoDomainMapper {

    public ServiceRequestBo toDomain(ServiceRequestDto serviceRequestDto){

        String orderReference = PaymentReference.getInstance().getNext();

        return ServiceRequestBo.serviceRequestBoWith()
            .orgId(serviceRequestDto.getHmctsOrgId())
            .ccdCaseNumber(serviceRequestDto.getCcdCaseNumber())
            .caseReference(serviceRequestDto.getCaseReference())
            .reference(orderReference)
            .fees(serviceRequestDto.getFees()
                .stream()
                .map(feeDto -> toFeeDomain(feeDto,serviceRequestDto.getCcdCaseNumber())) // Will be removed after get api's work without ccd dependency
                .collect(Collectors.toList()))
            .build();
    }

    public ServiceRequestFeeBo toFeeDomain(ServiceRequestFeeDto orderFeeDto, String ccdCaseNumber) {
        return ServiceRequestFeeBo.orderFeeBoWith()
            .calculatedAmount(orderFeeDto.getCalculatedAmount())
            .amountDue(orderFeeDto.getCalculatedAmount()) //amount due = calculated amount
            .code(orderFeeDto.getCode())
            .ccdCaseNumber(ccdCaseNumber)
            .version(orderFeeDto.getVersion())
            .volume(orderFeeDto.getVolume())
            .build();
    }
}
