package uk.gov.hmcts.payment.api.domain.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderFeeDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.List;

public interface OrderDomainService {

    PaymentFeeLink find(String orderReference);

    OrderBo create (OrderDto orderDto);

    void addFees (OrderBo orderBo, List<OrderFeeDto> feeDtos);

    OrderPaymentBo addPayments (PaymentFeeLink order, OrderPaymentDto orderPaymentDto) throws CheckDigitException;

    Boolean isDuplicate(String orderReference);
}
