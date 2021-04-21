package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.Arrays;

@Component
public class OrderPaymentDomainDataEntityMapper {

    private final static String PAYMENT_CHANNEL_ONLINE = "online";

    private final static String PAYMENT_METHOD = "payment by account";

    private final static String PAYMENT_METHOD_BY_ACCOUNT = "payment by account";

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private PaymentStatusRepository paymentStatusRepository;

    @Autowired
    private PaymentChannelRepository paymentChannelRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private Payment2Repository paymentRespository;

    @Autowired
    private ServiceIdSupplier serviceIdSupplier;

    @Autowired
    private UserIdSupplier userIdSupplier;

    public Payment toEntity(OrderPaymentBo paymentBo){

        return Payment.paymentWith()
            .userId(userIdSupplier.get())
            .s2sServiceName(serviceIdSupplier.get())
            .paymentChannel(paymentChannelRepository.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE))
            .paymentMethod(paymentMethodRepository.findByNameOrThrow(PAYMENT_METHOD_BY_ACCOUNT))
            .paymentStatus(paymentStatusRepository.findByNameOrThrow(paymentBo.getStatus().getName()))
            .reference(paymentBo.getReference())
            .status(paymentBo.getStatus().getName())
            .amount(paymentBo.getAmount())
            .pbaNumber(paymentBo.getAccountNumber())
            .statusHistories(paymentBo.getStatusHistories() == null ? Arrays.asList(StatusHistory.statusHistoryWith()
                .status(paymentStatusRepository.findByNameOrThrow(paymentBo.getStatus().getName()).getName())
                .build())
                : paymentBo.getStatusHistories())
            .build();
    }

    public OrderPaymentBo toDomain(Payment payment) {
        return OrderPaymentBo.orderPaymentBoWith()
            .reference(payment.getReference())
            .status(PaymentStatus.paymentStatusWith().name(payment.getStatus()).build())
            .dateCreated(payment.getDateCreated().toString())
            .build();
    }
}
