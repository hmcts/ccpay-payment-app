package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;

import java.util.stream.Collectors;

@Component
public class ServiceRequestDtoDomainMapper {

    public ServiceRequestBo toDomain(ServiceRequestDto serviceRequestDto, OrganisationalServiceDto organisationalServiceDto){

        String serviceRequestReference = PaymentReference.getInstance().getNext();

        return ServiceRequestBo.serviceRequestBoWith()
            .enterpriseServiceName(organisationalServiceDto.getServiceDescription())
            .orgId(organisationalServiceDto.getServiceCode())
            .ccdCaseNumber(serviceRequestDto.getCcdCaseNumber())
            .caseReference(serviceRequestDto.getCaseReference())
            .reference(serviceRequestReference)
            .fees(serviceRequestDto.getFees()
                .stream()
                .map(feeDto -> toFeeDomain(feeDto,serviceRequestDto.getCcdCaseNumber())) // Will be removed after get api's work without ccd dependency
                .collect(Collectors.toList()))
            .build();
    }

    public ServiceRequestFeeBo toFeeDomain(ServiceRequestFeeDto serviceRequestFeeDto, String ccdCaseNumber) {
        return ServiceRequestFeeBo.serviceRequestFeeBoWith()
            .calculatedAmount(serviceRequestFeeDto.getCalculatedAmount())
            .amountDue(serviceRequestFeeDto.getCalculatedAmount()) //amount due = calculated amount
            .code(serviceRequestFeeDto.getCode())
            .ccdCaseNumber(ccdCaseNumber)
            .version(serviceRequestFeeDto.getVersion())
            .volume(serviceRequestFeeDto.getVolume())
            .build();
    }
}
