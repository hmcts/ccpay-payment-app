package uk.gov.hmcts.payment.api.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.io.IOException;
import java.util.List;

public interface ServiceRequestDomainService {

    PaymentFeeLink find(String serviceRequestReference);

    List<PaymentFeeLink> findByCcdCaseNumber(String ccdCaseNumber);

    ServiceRequestResponseDto create(ServiceRequestDto serviceRequestDto, MultiValueMap<String, String> headers);

    ServiceRequestPaymentBo addPayments(PaymentFeeLink serviceRequest, String serviceRequestReference, ServiceRequestPaymentDto serviceRequestPaymentDto) throws CheckDigitException;

    OnlineCardPaymentResponse create(OnlineCardPaymentRequest onlineCardPaymentRequest, String serviceRequestReference, String returnURL, String serviceCallbackURL) throws CheckDigitException;

    PaymentFeeLink businessValidationForServiceRequests(PaymentFeeLink serviceRequest, ServiceRequestPaymentDto serviceRequestPaymentDto);

    ResponseEntity createIdempotencyRecord(ObjectMapper objectMapper, String idempotencyKey, String serviceRequestReference,
                                           String responseJson, ResponseEntity<?> responseEntity, ServiceRequestPaymentDto serviceRequestPaymentDto) throws JsonProcessingException;

    ResponseEntity createIdempotencyRecordWithoutResponse(ObjectMapper objectMapper, String idempotencyKey, String serviceRequestReference,
                                            ServiceRequestPaymentDto serviceRequestPaymentDto) throws JsonProcessingException;

    ResponseEntity updateIdempotencyRecord(ObjectMapper objectMapper, String idempotencyKey, String responseJson, ResponseEntity<?> responseEntity, String serviceRequestReference,
                                           ServiceRequestPaymentDto serviceRequestPaymentDto) throws JsonProcessingException;

    Boolean isDuplicate(String serviceRequestReference);

    void sendMessageTopicCPO(ServiceRequestDto serviceRequestDto, String serviceRequestReference);

    void sendMessageToTopic(PaymentStatusDto payment, String callBackUrl);

    void deadLetterProcess(IMessageReceiver subscriptionClient) throws ServiceBusException, InterruptedException, IOException;

    IMessageReceiver createDLQConnection() throws ServiceBusException, InterruptedException;
}
