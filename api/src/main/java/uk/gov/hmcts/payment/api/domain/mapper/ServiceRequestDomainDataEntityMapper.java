package uk.gov.hmcts.payment.api.domain.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
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

    public Payment toPaymentEntity(ServiceRequestOnlinePaymentBo requestOnlinePaymentBo, GovPayPayment govPayPayment) {
        return Payment.paymentWith()
            .userId(requestOnlinePaymentBo.getUserId())
            .s2sServiceName(requestOnlinePaymentBo.getS2sServiceName())
            .reference(requestOnlinePaymentBo.getPaymentReference())
            .amount(new BigDecimal(govPayPayment.getAmount()))
            .paymentChannel(PaymentChannel.ONLINE)
            .paymentProvider(PaymentProvider.GOV_PAY)
            .status(govPayPayment.getState().getStatus())
            .paymentStatus(PaymentStatus.paymentStatusWith().name(govPayPayment.getState().getStatus().toLowerCase()).build())
            .finished(govPayPayment.getState().getFinished())
            .externalReference(govPayPayment.getPaymentId())
            .description(govPayPayment.getDescription())
            .serviceCallbackUrl(requestOnlinePaymentBo.getServiceCallbackUrl())
            .returnUrl(govPayPayment.getReturnUrl())
            .nextUrl(hrefFor(govPayPayment.getLinks().getNextUrl()))
            .cancelUrl(hrefFor(govPayPayment.getLinks().getCancel()))
            .refundsUrl(hrefFor(govPayPayment.getLinks().getRefunds()))
            .statusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .externalStatus(govPayPayment.getState().getStatus().toLowerCase())
                .status(PayStatusToPayHubStatus.valueOf(govPayPayment.getState().getStatus().toLowerCase()).getMappedStatus())
                .errorCode(govPayPayment.getState().getCode())
                .message(govPayPayment.getState().getMessage())
                .build()))
            .build();
    }

    private String hrefFor(Link url) {
        return url == null ? null : url.getHref();
    }

}
