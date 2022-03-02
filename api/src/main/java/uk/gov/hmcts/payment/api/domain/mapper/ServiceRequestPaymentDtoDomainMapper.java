package uk.gov.hmcts.payment.api.domain.mapper;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

@Component
public class ServiceRequestPaymentDtoDomainMapper {

    @Autowired
    private ReferenceUtil referenceUtil;

    public ServiceRequestPaymentBo toDomain(ServiceRequestPaymentDto paymentDto) throws CheckDigitException {

        String reference = referenceUtil.getNext("RC");

        return ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
            .paymentReference(reference)
            .accountNumber(paymentDto.getAccountNumber())
            .amount(paymentDto.getAmount())
            .currency(CurrencyCode.valueOf(paymentDto.getCurrency()))
            .customerReference(paymentDto.getCustomerReference())
            .organisationName(paymentDto.getOrganisationName())
            .build();
    }
}
