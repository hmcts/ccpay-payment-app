package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderFeeBo;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;

import java.util.stream.Collectors;

@Component
public class OrderDtoDomainMapper {

    public OrderBo toDomain(OrderDto orderDto){

        String orderReference = PaymentReference.getInstance().getNext();

        return OrderBo.orderBoWith()
            .ccdCaseNumber(orderDto.getCcdCaseNumber())
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
