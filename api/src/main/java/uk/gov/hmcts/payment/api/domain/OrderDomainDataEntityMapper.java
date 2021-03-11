package uk.gov.hmcts.payment.api.domain;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.stream.Collectors;

@Component
public class OrderDomainDataEntityMapper {

    public PaymentFeeLink toEntity(OrderBo orderBo){

        return PaymentFeeLink.paymentFeeLinkWith()
            .ccdCaseNumber(orderBo.getCcdCaseNo())
            .paymentReference(orderBo.getReference())
            .fees(orderBo.getFees()
                .stream()
                .map(feeBo -> toEntity(feeBo))
                .collect(Collectors.toList()))
            .build();
    }

    public PaymentFee toEntity(OrderFeeBo orderFeeBo) {
        return PaymentFee.feeWith()
            .calculatedAmount(orderFeeBo.getCalculatedAmount())
            .code(orderFeeBo.getCode())
            .version(orderFeeBo.getVersion())
            .volume(orderFeeBo.getVolume())
            .build();

        //return Optional.of(orderFeeDto).map(OrderFeeBo :: );
    }
}
