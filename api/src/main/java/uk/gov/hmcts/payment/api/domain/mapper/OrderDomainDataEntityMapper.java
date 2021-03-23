package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderFeeBo;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.stream.Collectors;

@Component
public class OrderDomainDataEntityMapper {

    public PaymentFeeLink toEntity(OrderBo orderBo){

        return PaymentFeeLink.paymentFeeLinkWith()
            .ccdCaseNumber(orderBo.getCcdCaseNumber())
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

    public OrderBo toDomain(PaymentFeeLink paymentFeeLink){

        return OrderBo.orderBoWith()
            .ccdCaseNumber(paymentFeeLink.getCcdCaseNumber())
            .reference(paymentFeeLink.getPaymentReference())
            .fees(paymentFeeLink.getFees()
                .stream()
                .map(fee -> toDomain(fee))
                .collect(Collectors.toList()))
            .build();
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
