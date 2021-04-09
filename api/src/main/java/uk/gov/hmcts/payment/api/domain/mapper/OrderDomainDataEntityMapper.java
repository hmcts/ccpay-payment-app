package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderFeeBo;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderDomainDataEntityMapper {

    public PaymentFeeLink toOrderEntity(OrderBo orderBo) {

        return PaymentFeeLink.paymentFeeLinkWith()
            .orgId(orderBo.getOrgId())
            .enterpriseServiceName(orderBo.getEnterpriseServiceName())
            .paymentReference(orderBo.getReference())
            .ccdCaseNumber(orderBo.getCcdCaseNumber())
            .fees(orderBo.getFees()
                .stream()
                .map(feeBo -> toEntity(feeBo))
                .collect(Collectors.toList()))
            .build();
    }

    public CaseDetails toCaseDetailsEntity(OrderBo orderBo) {

        return CaseDetails.caseDetailsWith()
            .orders(new HashSet<>())
            .caseReference(orderBo.getCaseReference())
            .ccdCaseNumber(orderBo.getCcdCaseNumber())
            .build();
    }

    public PaymentFee toEntity(OrderFeeBo orderFeeBo) {

        return PaymentFee.feeWith()
            .calculatedAmount(orderFeeBo.getCalculatedAmount())
            .code(orderFeeBo.getCode())
            .version(orderFeeBo.getVersion())
            .volume(orderFeeBo.getVolume())
            .dateCreated(new Timestamp(System.currentTimeMillis()))
            .build();

        //return Optional.of(orderFeeDto).map(OrderFeeBo :: );
    }

    public OrderFeeBo toDomain(PaymentFee paymentFee) {

        return OrderFeeBo.orderFeeBoWith()
            .calculatedAmount(paymentFee.getCalculatedAmount())
            .code(paymentFee.getCode())
            .version(paymentFee.getVersion())
            .volume(paymentFee.getVolume())
            .build();

        //return Optional.of(orderFeeDto).map(OrderFeeBo :: );
    }
}
