package uk.gov.hmcts.payment.api.domain.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.domain.mapper.OrderDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.OrderBo;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.order.OrderDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;

import java.util.Map;
import java.util.Optional;

@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    @Autowired
    private OrderDtoDomainMapper orderDtoDomainMapper;

    @Autowired
    private OrderPaymentDtoDomainMapper orderPaymentDtoDomainMapper;

    @Autowired
    private OrderPaymentDomainDataEntityMapper orderPaymentDomainDataEntityMapper;

    @Autowired
    private Payment2Repository paymentRepository;

    @Autowired
    private ReferenceDataService referenceDataService;

    @Autowired
    private PaymentGroupService paymentGroupService;

    @Autowired
    private OrderBo orderBo;

    @Override
    public PaymentFeeLink find(String orderReference) {
        return (PaymentFeeLink) paymentGroupService.findByPaymentGroupReference(orderReference);
    }

    @Override
    @Transactional
    public Map<String ,Object> create(OrderDto orderDto, MultiValueMap<String, String> headers) {

        OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(orderDto.getCaseType(), headers);

        OrderBo orderBoDomain = orderDtoDomainMapper.toDomain(orderDto, organisationalServiceDto);
        return orderBo.createOrder(orderBoDomain);

    }

    @Override
    public OrderPaymentBo addPayments(PaymentFeeLink order, OrderPaymentDto orderPaymentDto) throws CheckDigitException {
        // TODO: 12/03/2021

        // 1. if canAcceptPayment return true -> Persist the Payment as Initiated Status
        OrderPaymentBo orderPaymentBo = orderPaymentDtoDomainMapper.toDomain(orderPaymentDto);
        orderPaymentBo.setStatus(PaymentStatus.CREATED);
        Payment payment = orderPaymentDomainDataEntityMapper.toEntity(orderPaymentBo);
        payment.setPaymentLink(order);
        paymentRepository.save(payment);

        // 2. Auto-Apportionment of Payment against Order Fees


        // 3. Invoke Liberata for real-time account Status
        // 4. update Payment Status
        // 5. update Fee Amount Due

        payment.setStatus(PaymentStatus.CREATED.getName());

        orderPaymentBo = orderPaymentDomainDataEntityMapper.toDomain(payment);
        return orderPaymentBo;
    }

    @Override
    public Boolean isDuplicate(String orderReference) {
        return Optional.of(find(orderReference)).isPresent();
    }
}
