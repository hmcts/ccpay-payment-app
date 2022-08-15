package uk.gov.hmcts.payment.api.service;

import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentFailureReportMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.exception.FailureReferenceNotFoundException;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.exception.InvalidPaymentFailureRequestException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFailures;
import uk.gov.hmcts.payment.api.model.PaymentFailureRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentStatusUpdateServiceImpl implements PaymentStatusUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusUpdateServiceImpl.class);

    @Autowired
    PaymentStatusDtoMapper paymentStatusDtoMapper;

    @Autowired
    PaymentFailureRepository paymentFailureRepository;

    @Autowired
    private Payment2Repository paymentRepository;

    @Autowired()
    @Qualifier("restTemplateRefundCancel")
    private RestTemplate restTemplateRefundCancel;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Value("${refund.api.url}")
    private String refundApiUrl;

    @Autowired
    private PaymentFailureReportMapper paymentFailureReportMapper;

    @Autowired()
    @Qualifier("restTemplateGetRefund")
    private RestTemplate restTemplateGetRefund;

    public PaymentFailures insertBounceChequePaymentFailure(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto) {

        LOG.info("Begin Payment failure insert in payment_failure table: {}", paymentStatusBouncedChequeDto.getPaymentReference());

        Optional<Payment> payment = paymentRepository.findByReference(paymentStatusBouncedChequeDto.getPaymentReference());

        if(payment.isEmpty()){
            throw new PaymentNotFoundException("No Payments available for the given Payment reference");
        }

        validatePaymentFailureAmount(paymentStatusBouncedChequeDto.getAmount(),payment.get());

        PaymentFailures paymentFailuresMap = paymentStatusDtoMapper.bounceChequeRequestMapper(paymentStatusBouncedChequeDto);
        try{

            PaymentFailures insertPaymentFailure = paymentFailureRepository.save(paymentFailuresMap);

            LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusBouncedChequeDto.getPaymentReference());
            return insertPaymentFailure;
        }catch(DataIntegrityViolationException e){
            throw new FailureReferenceNotFoundException("Request already received for this failure reference");
        }
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

        Optional<Payment> payment = paymentRepository.findByReference(paymentStatusChargebackDto.getPaymentReference());

        if(payment.isEmpty()){
            throw new PaymentNotFoundException("No Payments available for the given Payment reference");
        }

        validatePaymentFailureAmount(paymentStatusChargebackDto.getAmount(),payment.get());
        PaymentFailures paymentFailuresMap = paymentStatusDtoMapper.ChargebackRequestMapper(paymentStatusChargebackDto);

        try{
            PaymentFailures insertPaymentFailure = paymentFailureRepository.save(paymentFailuresMap);
            LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusChargebackDto.getPaymentReference());
            return insertPaymentFailure;
        } catch(DataIntegrityViolationException e){
            throw new FailureReferenceNotFoundException("Request already received for this failure reference");
        }
    }

    public List<PaymentFailures> searchPaymentFailure(String paymentReference) throws RestClientException{

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

    private void validatePaymentFailureAmount(BigDecimal disputeAmount, Payment payment){
        if(disputeAmount.compareTo(payment.getAmount()) > 0){
            throw new InvalidPaymentFailureRequestException("Failure amount can not be more than payment amount");
        }
    }

    @Override
    public PaymentFailures updatePaymentFailure(String failureReference, PaymentStatusUpdateSecond paymentStatusUpdateSecond) {

        if (null != paymentStatusUpdateSecond && null != paymentStatusUpdateSecond.getRepresentmentStatus() &&
                null != paymentStatusUpdateSecond.getRepresentmentDate() &&
                !paymentStatusUpdateSecond.getRepresentmentDate().isEmpty()) {

            Optional<PaymentFailures> paymentFailure = paymentFailureRepository.findByFailureReference(failureReference);

            if (!paymentFailure.isPresent()) {
                throw new PaymentNotFoundException("No Payment Failure available for the given Failure reference");
            } else {
                paymentFailure.get()
                        .setRepresentmentSuccess(paymentStatusUpdateSecond.getRepresentmentStatus().getStatus());
                paymentFailure.get()
                        .setRepresentmentOutcomeDate(
                                DateTime.parse(paymentStatusUpdateSecond.getRepresentmentDate()).withZone(
                                        DateTimeZone.UTC)
                                        .toDate());
                PaymentFailures updatedPaymentFailure = paymentFailureRepository.save(paymentFailure.get());
                LOG.info("Updated Payment failure record in payment_failure table: {}", failureReference);
                return updatedPaymentFailure;
            }
        } else {
            throw new InvalidPaymentFailureRequestException("Bad request");
        }
    }

    public List<PaymentFailureReportDto> paymentFailureReport(Date startDate,Date endDate,MultiValueMap<String, String> headers){
        LOG.info("Enter paymentFailureReport method");

        List<RefundDto> refundList = null;
        List<PaymentFailureReportDto> failureReport = new ArrayList<>();
        ValidationErrorDTO validationError = new ValidationErrorDTO();
        RefundPaymentFailureReportDtoResponse refundPaymentFailureReportDtoResponse = null;

        if(startDate.after(endDate)){
            validationError.addFieldError("dates", "Start date cannot be greater than end date");
            throw new ValidationErrorException("Error occurred in the report ", validationError);
        }

        List<PaymentFailures> paymentFailuresList = paymentFailureRepository.findByDatesBetween(startDate, endDate);

        if(paymentFailuresList.isEmpty()){
            throw new PaymentNotFoundException("No Data found to generate Report");
        }

        List<String> paymentReference= paymentFailuresList.stream().map(r->r.getPaymentReference()).distinct().collect(Collectors.toList());

        List<Payment> paymentList = paymentRepository.findByReferenceIn(paymentReference);

         if(paymentList.size() > 0){
            refundPaymentFailureReportDtoResponse = fetchRefundResponse(paymentReference);
        }

        if(null != refundPaymentFailureReportDtoResponse){
            refundList = refundPaymentFailureReportDtoResponse.getPaymentFailureDto();
        }

        List<RefundDto> finalRefundList = refundList;
        paymentFailuresList.stream()
            .collect(Collectors.toList())
            .forEach(paymentFailure -> {
                LOG.info("paymentFailure: {}", paymentFailure);
                failureReport.add(paymentFailureReportMapper.failureReportMapper(
                    paymentFailure,
                    paymentList.stream()
                        .filter(dto -> dto.getReference().equals(paymentFailure.getPaymentReference()))
                        .findAny().orElse(null),
                    finalRefundList
                ));
            });
        return failureReport;
    }

    public RefundPaymentFailureReportDtoResponse fetchRefundResponse(List<String> paymentReference) {

        try {

            ResponseEntity<RefundPaymentFailureReportDtoResponse> refundResponse =
                fetchFailedPaymentRefunds(paymentReference);
            LOG.info("Refund response status code {}", refundResponse.getStatusCode());
            LOG.info("Refund response {}", refundResponse.getBody());
            return refundResponse.hasBody() ? refundResponse.getBody() :null;
        } catch (HttpClientErrorException httpClientErrorException) {
            if (httpClientErrorException.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOG.info("Refund does not exist for the payment");
                return null;
            }
            throw new InvalidRefundRequestException(httpClientErrorException.getResponseBodyAsString());
        }
    }

    private ResponseEntity<RefundPaymentFailureReportDtoResponse> fetchFailedPaymentRefunds(List<String> paymentReference ) {
        String referenceId = paymentReference.stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refundApiUrl + "/refund/payment-failure-report")
            .queryParam("paymentReferenceList", referenceId);

        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        MultiValueMap<String, String> headerMultiValueMapForRefund = new LinkedMultiValueMap<>();
        //Service token
        headerMultiValueMapForRefund.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        headerMultiValueMapForRefund.put("Content-Type", List.of("application/json"));
        HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMapForRefund);
        final HttpEntity<List<RefundDto>> entity = new HttpEntity<>(httpHeaders);

        return restTemplateGetRefund.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<RefundPaymentFailureReportDtoResponse>() {
        });
    }

}
