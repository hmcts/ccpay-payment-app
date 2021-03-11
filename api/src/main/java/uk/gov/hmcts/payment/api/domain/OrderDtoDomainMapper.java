package uk.gov.hmcts.payment.api.domain;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.contract.OrderFeeDto;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.dto.OrderDto;

import java.util.stream.Collectors;

@Component
public class OrderDtoDomainMapper {

    public OrderBo toDomain(OrderDto orderDto){

        String orderReference = PaymentReference.getInstance().getNext();

        return OrderBo.orderBoWith()
            .ccdCaseNo(orderDto.getCcdCaseNo())
            .reference(orderReference)
            .fees(orderDto.getFees()
                .stream()
                .map(feeDto -> toDomain(feeDto))
                .collect(Collectors.toList()))
            .build();
    }

    public OrderFeeBo toDomain(OrderFeeDto orderFeeDto) {
        return OrderFeeBo.orderFeeBoWith()
            .calculatedAmount(orderFeeDto.getCalculatedAmount())
            .code(orderFeeDto.getCode())
            .version(orderFeeDto.getVersion())
            .volume(orderFeeDto.getVolume())
            .build();

        //return Optional.of(orderFeeDto).map(OrderFeeBo :: );
    }
}
