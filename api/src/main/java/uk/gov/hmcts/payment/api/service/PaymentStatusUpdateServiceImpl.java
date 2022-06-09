package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.controllers.ServiceRequestController;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.FailureTypeDto;
import uk.gov.hmcts.payment.api.dto.PaymentFailureStatusDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatusDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentStatusDtoMapper;
import uk.gov.hmcts.payment.api.dto.servicerequest.PaymentStatusBouncedChequeDto;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentStatusUpdateServiceImpl implements PaymentStatusUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentStatusUpdateServiceImpl.class);
    @Autowired
    PaymentStatusDtoMapper paymentStatusDtoMapper;
   // @Autowired
    //PaymentFailures paymentFailures;
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

    private final ServiceRequestDomainService serviceRequestDomainService;

    @Autowired()
    @Qualifier("restTemplateRefundCancel")
    private RestTemplate restTemplateRefundCancel;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Value("${refund.api.url}")
    private String refundApiUrl;

    @Autowired
    public PaymentStatusUpdateServiceImpl(
        ServiceRequestDomainService serviceRequestDomainService) {
        this.serviceRequestDomainService = serviceRequestDomainService;
    }

    public PaymentFailures insertBounceChequePaymentFailure(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto){

          PaymentFailures paymentFailures = paymentStatusDtoMapper.bounceChequeRequestMapper(paymentStatusBouncedChequeDto);
          PaymentFailures insertpaymentFailures=  paymentFailureRepository.save(paymentFailures);
          return insertpaymentFailures;
    }

    public Optional<PaymentFailures> searchFailureReference(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto){
        Optional<PaymentFailures> paymentFailures=  paymentFailureRepository.findByFailureReference(paymentStatusBouncedChequeDto.getFailureReference());

        return paymentFailures;

    }

    public void sendMessageToTopic(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto) throws JsonProcessingException{

        Payment payment = paymentService.findSavedPayment(paymentStatusBouncedChequeDto.getPaymentReference());
        List<FeePayApportion> feePayApportionList = paymentService.findByPaymentId(payment.getId());
        if(feePayApportionList.isEmpty()){
            throw new PaymentNotSuccessException("Payment is not successful");
        }
        List<PaymentFee> fees = feePayApportionList.stream().map(feePayApportion ->feeService.getPaymentFee(feePayApportion.getFeeId()).get())
            .collect(Collectors.toSet()).stream().collect(Collectors.toList());
        PaymentFeeLink paymentFeeLink = fees.get(0).getPaymentLink();
         LOG.info("paymentFeeLink getEnterpriseServiceName {}",paymentFeeLink.getEnterpriseServiceName());
         LOG.info("paymentFeeLink getCcdCaseNumber {}",paymentFeeLink.getCcdCaseNumber());
        PaymentFeeLink  retrieveDelegatingPaymentService = delegatingPaymentService.retrieve(paymentFeeLink, payment.getReference());
        String serviceRequestStatus = paymentGroup.toPaymentGroupDto(retrieveDelegatingPaymentService).getServiceRequestStatus();
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String serviceRequestReference = paymentFeeLink.getPaymentReference();
        PaymentFailureStatusDto paymentFailureStatusDto = paymentDtoMapper.toPaymentFailureStatusDto(serviceRequestReference, "", payment, serviceRequestStatus,paymentStatusBouncedChequeDto.getAmount() );
        serviceRequestDomainService.sendFailureMessageToTopic(paymentFailureStatusDto, paymentFeeLink.getCallBackUrl());

        String jsonpaymentStatusDto = ow.writeValueAsString(paymentFailureStatusDto);
        LOG.info("json format paymentFailureStatusDto to Topic {}",jsonpaymentStatusDto);
        LOG.info("callback URL paymentFailureStatusDto to Topic {}",paymentFeeLink.getCallBackUrl());

    }

    public boolean cancelFailurePaymentRefund(PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto){

        try {
            ResponseEntity<String> updateRefundStatus = CancelRefund(paymentStatusBouncedChequeDto.getPaymentReference());

           if (null != updateRefundStatus && updateRefundStatus.getStatusCode().is2xxSuccessful()) {
                return true;
            }

        } catch (HttpClientErrorException httpClientErrorException) {
            throw new PaymentException("Refund Can not make as cancel " + paymentStatusBouncedChequeDto.getPaymentReference() +
                " Due to response status code as  = " + httpClientErrorException.getMessage());
        } catch (Exception exception) {
            throw new PaymentException("Error occurred while processing refund cancel " + paymentStatusBouncedChequeDto.getPaymentReference() +
                " Exception message  = " + exception.getMessage());
        }
        return false;
    }

    public ResponseEntity<String> CancelRefund(String paymentReference) throws RestClientException {

        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        MultiValueMap<String, String> headerMultiValueMapForRefund = new LinkedMultiValueMap<String, String>();
        //Service token
        headerMultiValueMapForRefund.put("ServiceAuthorization", serviceAuthTokenPaymentList);

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForRefund);
        final HttpEntity<String> entity = new HttpEntity<>(headers);
        Map<String, String> params = new HashMap<>();
        params.put("paymentReference", paymentReference);
//        return new ResponseEntity<String>("new ArrayList<>()", HttpStatus.OK);
        LOG.info("Calling Refund  api to cancel refund for failed payment");
        return restTemplateRefundCancel.exchange(refundApiUrl + "/payment/{paymentReference}/action/cancel", HttpMethod.PATCH, entity, String.class, params);




       /* UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(new StringBuilder(refundApiUrl).append(
            "/payment/").append(paymentReference)+"/action/cancel");
        LOG.info("URI {}", builder.toUriString());
        LOG.info("Calling refund api to cancel failed payment");
        FailureTypeDto failureTypeDto = toFailureType("Bounced Cheque");
        return restTemplateRefundCancel
            .exchange(
                builder.toUriString(),
                HttpMethod.PATCH,
                getHttpEntityForCancelRefundsPatch(failureType),
                String.class
            );*/
    }

    private HttpEntity<String> getHeaders() {

        return new HttpEntity<>(getFormatedHeadersForS2S());
    }

    private MultiValueMap<String, String> getFormatedHeadersForS2S() {
        List<String> serviceauthtoken = Arrays.asList(authTokenGenerator.generate());

        MultiValueMap<String, String> inputHeaders = new LinkedMultiValueMap<>();
        inputHeaders.put("ServiceAuthorization", serviceauthtoken);

        return inputHeaders;
    }

    private HttpEntity<String> getHttpEntityForCancelRefundsPatch(String failureType) {
        return new HttpEntity<>(failureType, getFormatedHeadersForS2S());
    }

    private FailureTypeDto toFailureType(String type) {

        return FailureTypeDto.paymentFailureType()
            .failureType(type)
            .build();
    }

}
