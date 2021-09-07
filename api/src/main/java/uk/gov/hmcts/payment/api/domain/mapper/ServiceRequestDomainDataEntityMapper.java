package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.sql.Timestamp;
import java.util.stream.Collectors;

@Component
public class ServiceRequestDomainDataEntityMapper {

    public PaymentFeeLink toServiceRequestEntity(ServiceRequestBo serviceRequestBo) {

        return PaymentFeeLink.paymentFeeLinkWith()
            .orgId(serviceRequestBo.getOrgId())
            .enterpriseServiceName(serviceRequestBo.getEnterpriseServiceName())
            .paymentReference(serviceRequestBo.getReference())
            .ccdCaseNumber(serviceRequestBo.getCcdCaseNumber())
            .caseReference(serviceRequestBo.getCaseReference())// Will be removed after get api's work without ccd dependency
            .fees(serviceRequestBo.getFees()
                .stream()
                .map(feeBo -> toPaymentFeeEntity(feeBo)) // Will be removed after get api's work without ccd dependency
                .collect(Collectors.toList()))
            .build();
    }

    public PaymentFee toPaymentFeeEntity(ServiceRequestFeeBo orderFeeBo) {

        return PaymentFee.feeWith()
            .calculatedAmount(orderFeeBo.getCalculatedAmount())
            .amountDue(orderFeeBo.getAmountDue())
            .code(orderFeeBo.getCode())
            .ccdCaseNumber(orderFeeBo.getCcdCaseNumber()) // Will be removed after get api's work without ccd dependency
            .version(orderFeeBo.getVersion())
            .volume(orderFeeBo.getVolume())
            .dateCreated(new Timestamp(System.currentTimeMillis()))
            .build();
    }

}
