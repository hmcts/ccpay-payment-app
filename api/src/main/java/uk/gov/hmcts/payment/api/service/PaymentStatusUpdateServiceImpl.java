package uk.gov.hmcts.payment.api.service;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.exception.FailureReferenceNotFoundException;
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
    private static final String TOO_MANY_RQUESTS_EXCEPTION_MSG = "Request already received for this failure reference";

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

        validatePaymentFailureAmount(paymentStatusChargebackDto.getAmount(),payment.get());
        PaymentFailures paymentFailuresMap = paymentStatusDtoMapper.ChargebackRequestMapper(paymentStatusChargebackDto);

        try{
            PaymentFailures insertPaymentFailure = paymentFailureRepository.save(paymentFailuresMap);
            LOG.info("Completed  Payment failure insert in payment_failure table: {}", paymentStatusChargebackDto.getPaymentReference());
            return insertPaymentFailure;
        } catch(DataIntegrityViolationException e){
            throw new FailureReferenceNotFoundException(TOO_MANY_RQUESTS_EXCEPTION_MSG);
        }
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

    private void validatePaymentFailureAmount(BigDecimal disputeAmount, Payment payment){
        if(disputeAmount.compareTo(payment.getAmount()) > 0){
            throw new InvalidPaymentFailureRequestException("Failure amount cannot be more than payment amount");
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

    public void updateUnprocessedPayment(){

        LOG.info("Inside updateUnprocessedPayment method");
        List<PaymentFailures> paymentFailuresListWithNoRC = paymentFailureRepository.findDcn();;

        LOG.info("Inside updateUnprocessedPayment method paymentFailuresListWithNoRC size :{}",paymentFailuresListWithNoRC.size());

        List<String> paymentFailuresListWithNoDcn = paymentFailuresListWithNoRC.stream().map(p -> p.getDcn()).collect(Collectors.toList());

        List<Payment> paymentList = paymentRepository.findByDocumentControlNumberIn(paymentFailuresListWithNoDcn);

        if(paymentList != null){
            paymentFailuresListWithNoRC.stream().forEach( paymentFailure -> {
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

    @Override
    public PaymentFailures unprocessedPayment(UnprocessedPayment unprocessedPayment,
                                              MultiValueMap<String, String> headers) {

        if (!validateDcn(unprocessedPayment.getDcn(), unprocessedPayment.getAmount(), headers)) {
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

    private boolean validateDcn(String dcn, BigDecimal amount, MultiValueMap<String, String> headers) {
        Map<String, String> params = new HashMap<>();
        params.put("document_control_number", dcn);
        LOG.info("Calling Bulk scan api to retrieve payment by dcn: {}", dcn);
        ResponseEntity responseEntity = restTemplatePaymentGroup
                .exchange(bulkScanPaymentsProcessedUrl + casesPath + "/{document_control_number}", HttpMethod.GET,
                        new HttpEntity<>(headers), ResponseEntity.class, params);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return false;
        } else {
            SearchResponse searchResponse = (SearchResponse) responseEntity.getBody();
            LOG.info("Search Response from Bulk Scanning app: {}", searchResponse);
            if (null != searchResponse && !PaymentStatus.PROCESSED.equals(searchResponse.getAllPaymentsStatus())) {
                for (PaymentMetadataDto paymentMetadataDto : searchResponse.getPayments()) {
                    if (dcn.equals(paymentMetadataDto.getDcnReference()) && paymentMetadataDto.getAmount().compareTo(amount) > 0) {
                        throw new InvalidPaymentFailureRequestException("Failure amount cannot be more than payment amount");
                    }
                }
            }
        }
        return true;
    }
}
