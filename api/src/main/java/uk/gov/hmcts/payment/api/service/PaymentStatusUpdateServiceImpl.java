package uk.gov.hmcts.payment.api.service;

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
import uk.gov.hmcts.payment.api.dto.PaymentStatus;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentFailureReportMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.exception.FailureReferenceNotFoundException;
import uk.gov.hmcts.payment.api.exception.InvalidPaymentFailureRequestException;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentStatusUpdateServiceImpl implements PaymentStatusUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusUpdateServiceImpl.class);
    private static final String TOO_MANY_RQUESTS_EXCEPTION_MSG = "Request already received for this failure reference";
    private static final String PAYMENT_METHOD = "cheque";
    private static final String FAILURE_AMOUNT_VALIDATION = "Failure amount is more than the possible amount";

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

    @Autowired()
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplatePaymentGroup;

    @Value("${bulk.scanning.payments.processed.url}")
    private String bulkScanPaymentsProcessedUrl;

    @Value("${bulk.scanning.cases-path}")
    private String casesPath;

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

        validateBounceChequeRequest(paymentStatusBouncedChequeDto, payment.get());
        validatePingOneDate(paymentStatusBouncedChequeDto.getEventDateTime(), payment.get().getDateUpdated());

        PaymentFailures paymentFailuresMap = paymentStatusDtoMapper.bounceChequeRequestMapper(paymentStatusBouncedChequeDto, payment.get());
        try{

            PaymentFailures insertPaymentFailure = paymentFailureRepository.save(paymentFailuresMap);

            LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusBouncedChequeDto.getPaymentReference());
            return insertPaymentFailure;
        }catch(DataIntegrityViolationException e){
            throw new FailureReferenceNotFoundException(TOO_MANY_RQUESTS_EXCEPTION_MSG);
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

        validatePaymentFailureAmount(paymentStatusChargebackDto,payment.get());
        validatePingOneDate(paymentStatusChargebackDto.getEventDateTime(), payment.get().getDateUpdated());
        PaymentFailures paymentFailuresMap = paymentStatusDtoMapper.ChargebackRequestMapper(paymentStatusChargebackDto);

        try{
            PaymentFailures insertPaymentFailure = paymentFailureRepository.save(paymentFailuresMap);
            LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusChargebackDto.getPaymentReference());
            return insertPaymentFailure;
        } catch(DataIntegrityViolationException e){
            throw new FailureReferenceNotFoundException(TOO_MANY_RQUESTS_EXCEPTION_MSG);
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

    private void validatePaymentFailureAmount(PaymentStatusChargebackDto paymentStatusChargebackDto, Payment payment){

        if (paymentStatusChargebackDto.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new InvalidPaymentFailureRequestException(FAILURE_AMOUNT_VALIDATION);
        }

        Optional < List < PaymentFailures >> paymentFailuresList = paymentFailureRepository.findByPaymentReference(paymentStatusChargebackDto.getPaymentReference());

        BigDecimal totalDisputeAmount = BigDecimal.ZERO;

        if (paymentFailuresList.isPresent()) {

            for (PaymentFailures paymentFailure: paymentFailuresList.get()) {

                totalDisputeAmount = paymentFailure.getAmount().add(totalDisputeAmount);
            }

            totalDisputeAmount = paymentStatusChargebackDto.getAmount().add(totalDisputeAmount);
        }

        if (totalDisputeAmount.compareTo(payment.getAmount()) > 0) {
            throw new InvalidPaymentFailureRequestException(FAILURE_AMOUNT_VALIDATION);
        }
    }

    private void validateBounceChequeRequest(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto, Payment payment){

        if (!(payment.getPaymentMethod().getName().equals(PAYMENT_METHOD))){
            throw new InvalidPaymentFailureRequestException("Incorrect payment method");
        }

        int amountCompare = paymentStatusBouncedChequeDto.getAmount().compareTo(payment.getAmount());

        if (amountCompare != 0) {
            throw new InvalidPaymentFailureRequestException("Dispute amount can not be less than payment amount");
        }
    }

    private void validatePingOneDate(String pingOneDateStr, Date paymentDate){

       Date pingOneDate =  DateTime.parse(pingOneDateStr).withZone(DateTimeZone.UTC).toDate();
        if (pingOneDate.before(paymentDate)){
            throw new InvalidPaymentFailureRequestException("Failure event date can not be prior to payment date");
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
                Date pingTwoDate =  DateTime.parse(paymentStatusUpdateSecond.getRepresentmentDate()).withZone(DateTimeZone.UTC).toDate();

                LOG.info("pingTwoDate time{}", pingTwoDate.getTime());
                LOG.info("representation Date time{}", DateTime.parse(paymentStatusUpdateSecond.getRepresentmentDate()));
                LOG.info("getFailureEventDateTime time{}", paymentFailure.get().getFailureEventDateTime().getTime());
                if (pingTwoDate.before(paymentFailure.get().getFailureEventDateTime())){
                    throw new InvalidPaymentFailureRequestException("Representment date can not be prior to failure event date");
                }
                if (paymentStatusUpdateSecond.getRepresentmentStatus().equals(RepresentmentStatus.Yes)
                    && paymentFailure.get().getFailureType().equals("Chargeback")) {
                    paymentFailure.get().setHasAmountDebited("No");
                }
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

        List<String> paymentReference= paymentFailuresList.stream().map(PaymentFailures::getPaymentReference).distinct().collect(Collectors.toList());

        List<Payment> paymentList = paymentRepository.findByReferenceIn(paymentReference);
        List<String> paymentRefForRefund = paymentList.stream().map(Payment::getReference).collect(Collectors.toList());

         if(paymentList.size() > 0){
            refundPaymentFailureReportDtoResponse = fetchRefundResponse(paymentRefForRefund);
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

        return restTemplateGetRefund.exchange(builder.toUriString(), HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {
                });
    }


    @Override
    public PaymentFailures unprocessedPayment(UnprocessedPayment unprocessedPayment,
                                              MultiValueMap<String, String> headers) {

        if (!validateDcn(unprocessedPayment, headers)) {
            throw new PaymentNotFoundException("No Payments available for the given document reference number");
        }

        PaymentFailures paymentFailure = paymentStatusDtoMapper.unprocessedPaymentMapper(unprocessedPayment);
        try {
            PaymentFailures savedPaymentFailure = paymentFailureRepository.save(paymentFailure);
            LOG.info("Completed Payment failure insert in payment_failure table: {}", unprocessedPayment.getDcn());
            return savedPaymentFailure;
        } catch (DataIntegrityViolationException e) {
            throw new FailureReferenceNotFoundException(TOO_MANY_RQUESTS_EXCEPTION_MSG);
        }
    }

    private boolean validateDcn(UnprocessedPayment unprocessedPayment, MultiValueMap<String, String> headers) {
        String dcn = unprocessedPayment.getDcn();
        BigDecimal amount = unprocessedPayment.getAmount();

        String bsURL = bulkScanPaymentsProcessedUrl + casesPath + "/{document_control_number}?internalFlag=true";
        Map<String, String> params = new HashMap<>();
        params.put("document_control_number", dcn);
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("serviceauthorization", headers.get("serviceauthorization"));
        LOG.info("Headers: {}", header);
        HttpHeaders headersVal = new HttpHeaders(header);
        final HttpEntity<String> entity = new HttpEntity<>(headersVal);
        ResponseEntity<SearchResponse> responseEntity;
        try {
            LOG.info("Calling Bulk scan api to retrieve payment: {}", bsURL);
            LOG.info("restTemplatePaymentGroup before Calling Bulkscan : {}",restTemplatePaymentGroup);
            responseEntity = restTemplatePaymentGroup.exchange(bsURL, HttpMethod.GET, entity, SearchResponse.class, params);
            LOG.info("Response Entity from BS Call: {}", responseEntity);
        } catch (HttpClientErrorException exception) {
            LOG.error("Exception occurred while calling bulk scan application: {}, {}",
                    exception.getMessage(), exception.getStackTrace());
            throw new PaymentNotFoundException("No Payments available for the given document reference number");
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return false;
        } else {
            SearchResponse searchResponse = responseEntity.getBody();
            LOG.info("Search Response from Bulk Scanning app: {}", searchResponse);
            validateUnprocessedPayment(searchResponse, unprocessedPayment, dcn, amount);
        }
        return true;
    }

    private void validateUnprocessedPayment(SearchResponse searchResponse,UnprocessedPayment unprocessedPayment, String dcn, BigDecimal amount){

        if (null != searchResponse && PaymentStatus.COMPLETE.equals(searchResponse.getAllPaymentsStatus())) {
            for (PaymentMetadataDto paymentMetadataDto : searchResponse.getPayments()) {
                LOG.info("dcn comparison: {}", dcn.equals(paymentMetadataDto.getDcnReference()));
                LOG.info("amount comparison: {}", amount.compareTo(paymentMetadataDto.getAmount()));
                if (dcn.equals(paymentMetadataDto.getDcnReference()) && amount.compareTo(paymentMetadataDto.getAmount()) > 0) {
                    throw new InvalidPaymentFailureRequestException("Failure amount cannot be more than payment amount");
                }
                if(dcn.equals(paymentMetadataDto.getDcnReference())) {
                    validatePingOneDate(unprocessedPayment.getEventDateTime(), paymentMetadataDto.getDateUpdated());
                }
            }
        }

    }

    @Transactional
    public void updateUnprocessedPayment(){

        LOG.info("Inside updateUnprocessedPayment method");
        List<PaymentFailures> paymentFailuresListWithNoRC = paymentFailureRepository.findDcn();

        LOG.info("Inside updateUnprocessedPayment method paymentFailuresListWithNoRC size :{}",paymentFailuresListWithNoRC.size());

        List<String> paymentFailuresListWithNoDcn = paymentFailuresListWithNoRC.stream().map(PaymentFailures::getDcn).collect(Collectors.toList());

        List<Payment> paymentList = paymentRepository.findByDocumentControlNumberInAndPaymentMethod(paymentFailuresListWithNoDcn, PaymentMethod.paymentMethodWith().name(PAYMENT_METHOD).build());

        if(paymentList != null){
            paymentFailuresListWithNoRC.forEach(paymentFailure -> {
                Payment payment =   paymentList.stream().filter(p -> p.getDocumentControlNumber().equals(paymentFailure.getDcn())).findFirst().orElse(null);
                if(payment != null){
                    updatePaymentReferenceForDcn(paymentFailure,payment);
                } });
        }
    }

    private void updatePaymentReferenceForDcn(PaymentFailures paymentFailure,Payment payment){

        Optional<PaymentFailures> paymentFailureResponse = paymentFailureRepository.findByFailureReference(paymentFailure.getFailureReference());
        if(paymentFailureResponse.isPresent()){
            paymentFailureResponse.get().setPaymentReference(payment.getReference());
            paymentFailureResponse.get().setCcdCaseNumber(payment.getCcdCaseNumber());
            paymentFailureRepository.save(paymentFailureResponse.get());

            LOG.info("updateUnprocessedPayment successful");
        }
    }

}
