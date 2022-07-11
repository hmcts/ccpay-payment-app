package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.CasePaymentOrderDto;
import uk.gov.hmcts.payment.api.contract.CasePaymentOrdersDto;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.IdamTokenResponse;
import uk.gov.hmcts.payment.api.dto.PaymentFailureStatusDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusChargebackDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusUpdateSecond;
import uk.gov.hmcts.payment.api.dto.mapper.CasePaymentOrdersMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.model.PaymentFailureRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.casepaymentorders.client.ServiceRequestCpoServiceClient;
import uk.gov.hmcts.payment.casepaymentorders.client.dto.CpoGetResponse;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.*;

@Service
public class PaymentStatusUpdateServiceImpl implements PaymentStatusUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusUpdateServiceImpl.class);

    @Autowired
    PaymentStatusDtoMapper paymentStatusDtoMapper;

    @Autowired
    PaymentFailureRepository paymentFailureRepository;

    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;

    @Autowired
    private FeesService feeService;

    @Autowired
    private PaymentGroupDtoMapper paymentGroup;

    @Autowired
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Autowired
    private PaymentDtoMapper paymentDtoMapper;

    @Autowired
    private ServiceRequestCpoServiceClient cpoServiceClient;

    @Autowired
    private IdamService idamService;

    private final ServiceRequestDomainService serviceRequestDomainService;

    @Autowired()
    @Qualifier("restTemplateRefundCancel")
    private RestTemplate restTemplateRefundCancel;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Value("${refund.api.url}")
    private String refundApiUrl;

    @Autowired
    private CasePaymentOrdersMapper casePaymentOrdersMapper;

    public static final String BEARER = "Bearer ";

    @Autowired
    public PaymentStatusUpdateServiceImpl(
        ServiceRequestDomainService serviceRequestDomainService) {
        this.serviceRequestDomainService = serviceRequestDomainService;
    }

    public PaymentFailures insertBounceChequePaymentFailure(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto) {

        LOG.info("Begin Payment failure insert in payment_failure table: {}", paymentStatusBouncedChequeDto.getPaymentReference());
        PaymentFailures paymentFailures = paymentStatusDtoMapper.bounceChequeRequestMapper(paymentStatusBouncedChequeDto);
        PaymentFailures insertpaymentFailures = paymentFailureRepository.save(paymentFailures);
        LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusBouncedChequeDto.getPaymentReference());
        return insertpaymentFailures;
    }

    public Optional<PaymentFailures> searchFailureReference(String failureReference) {
        return paymentFailureRepository.findByFailureReference(failureReference);
    }

    public void sendFailureMessageToServiceTopic(Payment payment, PaymentFailures paymentFailure) throws JsonProcessingException {

        PaymentFeeLink paymentFeeLink = payment.getPaymentLink();
        LOG.info("paymentFeeLink getCcdCaseNumber {}", paymentFeeLink.getCcdCaseNumber());
        PaymentFeeLink retrieveDelegatingPaymentService = delegatingPaymentService.retrieve(paymentFeeLink, payment.getReference());
        String serviceRequestStatus = paymentGroup.toPaymentFailureGroupDto(retrieveDelegatingPaymentService).getServiceRequestStatus();
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String serviceRequestReference = paymentFeeLink.getPaymentReference();
        CpoGetResponse casePaymentOrders = getCasePaymentOrders(payment.getCcdCaseNumber());
        LOG.info("CpoGetResponse {}", casePaymentOrders);
        CasePaymentOrdersDto casePaymentOrdersDto;
        CasePaymentOrderDto filterCasePaymentOrdersDto =null;
        if (null != casePaymentOrders) {
            casePaymentOrdersDto = casePaymentOrdersMapper.toCasePaymentOrdersDto(casePaymentOrders);
            filterCasePaymentOrdersDto = filterCasePaymentOrdersDto(casePaymentOrdersDto,serviceRequestReference);
        }
        PaymentFailureStatusDto paymentFailureStatusDto = paymentDtoMapper.toPaymentFailureStatusDto(serviceRequestReference, paymentFeeLink, serviceRequestStatus, filterCasePaymentOrdersDto, paymentFailure, payment);
        if (null != paymentFeeLink.getCallBackUrl()) {
            serviceRequestDomainService.sendFailureMessageToTopic(paymentFailureStatusDto, paymentFeeLink.getCallBackUrl());
        }
        String jsonpaymentStatusDto = ow.writeValueAsString(paymentFailureStatusDto);
        LOG.info("json format paymentFailureStatusDto to Topic {}", jsonpaymentStatusDto);
        LOG.info("callback URL paymentFailureStatusDto to Topic {}", paymentFeeLink.getCallBackUrl());

    }

    public boolean cancelFailurePaymentRefund(String paymentReference) {

        try {
            LOG.info("Enter cancelFailurePaymentRefund method:: {}", paymentReference);
            ResponseEntity<String> updateRefundStatus = cancelRefund(paymentReference);

            if (updateRefundStatus.getStatusCode().is2xxSuccessful()) {
                LOG.info("Refund cancelled successfully:: {}", paymentReference);
            }

        } catch (HttpClientErrorException httpClientErrorException) {

            if (httpClientErrorException.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOG.info("Refund does not exist for the payment:: {}", paymentReference);
            } else {
                LOG.error("Refund App unavailable. Please try again:: {}", paymentReference);
            }
        } catch (Exception exception) {
            LOG.error("Refund App unavailable. Please try again:: {}", paymentReference);
        }
        return true;
    }

    private ResponseEntity<String> cancelRefund(String paymentReference) throws RestClientException {

        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        MultiValueMap<String, String> headerMultiValueMapForRefund = new LinkedMultiValueMap<>();
        //Service token
        headerMultiValueMapForRefund.put("ServiceAuthorization", serviceAuthTokenPaymentList);

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForRefund);
        final HttpEntity<String> entity = new HttpEntity<>(headers);
        Map<String, String> params = new HashMap<>();
        params.put("paymentReference", paymentReference);
        LOG.info("Calling Refund  api to cancel refund for failed payment: {}", paymentReference);
        return restTemplateRefundCancel.exchange(refundApiUrl + "/payment/{paymentReference}/action/cancel", HttpMethod.PATCH, entity, String.class, params);
    }

    public PaymentFailures insertChargebackPaymentFailure(PaymentStatusChargebackDto paymentStatusChargebackDto) {

        LOG.info("Begin Payment failure insert in payment_failure table: {}", paymentStatusChargebackDto.getPaymentReference());
        PaymentFailures paymentFailures = paymentStatusDtoMapper.ChargebackRequestMapper(paymentStatusChargebackDto);
        PaymentFailures insertpaymentFailures = paymentFailureRepository.save(paymentFailures);
        LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusChargebackDto.getPaymentReference());
        return insertpaymentFailures;
    }

    public List<PaymentFailures> searchPaymentFailure(String paymentReference) {

        Optional<List<PaymentFailures>> paymentFailures;
        paymentFailures = paymentFailureRepository.findByPaymentReferenceOrderByFailureEventDateTimeDesc(paymentReference);
        if (paymentFailures.isPresent()) {
            return paymentFailures.get();
        }
        throw new PaymentNotFoundException("no record found");
    }

    @Override
    @Transactional
    public void deleteByFailureReference(String failureReference) {
        long records = paymentFailureRepository.deleteByFailureReference(failureReference);
        LOG.info("Number of deleted records are: {}", records);
        if (records == 0) {
            throw new PaymentNotFoundException("Failure reference not found in database for delete");
        }
    }

    @Override
    public PaymentFailures updatePaymentFailure(PaymentFailures paymentFailure, PaymentStatusUpdateSecond paymentStatusUpdateSecond) {
        paymentFailure.setRepresentmentSuccess(paymentStatusUpdateSecond.getRepresentmentStatus());
        paymentFailure.setRepresentmentOutcomeDate(paymentStatusUpdateSecond.getRepresentmentDate());
        PaymentFailures updatedpaymentFailure = paymentFailureRepository.save(paymentFailure);
        LOG.info("Updated Payment failure record in payment_failure table: {}", paymentFailure.getPaymentReference());
        return updatedpaymentFailure;
    }

    public CpoGetResponse getCasePaymentOrders(String caseIds) {
        return cpoServiceClient.getCasePaymentOrdersForServiceReq(caseIds, getAccessToken(),
            authTokenGenerator.generate());
    }

    private String getAccessToken() {
        IdamTokenResponse idamTokenResponse = idamService.getSecurityTokens();
        LOG.info("idamTokenResponse {}", idamTokenResponse.getAccessToken());
        return BEARER + idamTokenResponse.getAccessToken();
    }

    private CasePaymentOrderDto filterCasePaymentOrdersDto(CasePaymentOrdersDto casePaymentOrdersDto, String serviceRequestReference) {

        return casePaymentOrdersDto.getContent().stream()
        .filter(s -> serviceRequestReference.equalsIgnoreCase(s.getOrderReference())).findAny().orElse(null);
}

}
