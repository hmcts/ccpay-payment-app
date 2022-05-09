package uk.gov.hmcts.payment.api.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestCpoDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.DeadLetterDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;
import uk.gov.hmcts.payment.api.servicebus.TopicClientService;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoAmountDue;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoMatchingAmount;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServiceRequestDomainServiceImpl implements ServiceRequestDomainService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRequestDomainServiceImpl.class);
    private static final String FAILED = "failed";
    private static final String SUCCESS = "success";
    private static final String MSGCONTENTTYPE = "application/json";
    @Value("${case-payment-orders.api.url}")
    private  String callBackUrl;

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    private static final String topic = "ccpay-service-request-cpo-update-topic";

    private String topicCardPBA = "serviceCallbackTopic";

    @Autowired
    private ServiceRequestDtoDomainMapper serviceRequestDtoDomainMapper;

    @Autowired
    private ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

    @Autowired
    private PaymentGroupDtoMapper paymentGroup;

    @Autowired
    private ServiceRequestPaymentDtoDomainMapper serviceRequestPaymentDtoDomainMapper;

    @Autowired
    private ServiceRequestPaymentDomainDataEntityMapper serviceRequestPaymentDomainDataEntityMapper;

    @Autowired
    private Payment2Repository paymentRepository;

    @Autowired
    private ReferenceDataService referenceDataService;

    @Autowired
    private PaymentGroupService paymentGroupService;

    @Value("#{'${pba.config1.service.names}'.split(',')}")
    private List<String> pbaConfig1ServiceNames;

    @Autowired
    private AccountService<AccountDto, String> accountService;

    @Autowired
    private PBAStatusErrorMapper pbaStatusErrorMapper;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    @Autowired
    private FeePayApportionService feePayApportionService;

    @Autowired
    private ServiceRequestBo serviceRequestBo;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private IdempotencyKeysRepository idempotencyKeysRepository;

    @Autowired
    private DelegatingPaymentService<GovPayPayment, String> delegateGovPay;

    @Autowired
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Autowired
    private ServiceRequestDomainService serviceRequestDomainService;

    @Autowired
    private TopicClientService topicClientService;

    @Autowired
    PaymentDtoMapper paymentDtoMapper;

    private Function<PaymentFeeLink, Payment> getFirstSuccessPayment = serviceRequest -> serviceRequest.getPayments().stream().
        filter(payment -> payment.getPaymentStatus().getName().equalsIgnoreCase("success")).collect(Collectors.toList()).get(0);

    @Override
    public List<PaymentFeeLink> findByCcdCaseNumber(String ccdCaseNumber) {
        Optional<List<PaymentFeeLink>> paymentFeeLinks = paymentFeeLinkRepository.findByCcdCaseNumber(ccdCaseNumber);
        return paymentFeeLinks.orElseThrow(() -> new PaymentGroupNotFoundException("ServiceRequest detail not found for given ccdcasenumber " + ccdCaseNumber));
    }

    @Override
    public PaymentFeeLink find(String serviceRequestReference) {
        return (PaymentFeeLink) paymentGroupService.findByPaymentGroupReference(serviceRequestReference);
    }

    @Override
    @Transactional
    public ServiceRequestResponseDto create(ServiceRequestDto serviceRequestDto, MultiValueMap<String, String> headers) {
        LOG.info("Ref data from service request HMCTSorgid---- {} ",serviceRequestDto.getHmctsOrgId());
        OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(Optional.empty(),
            Optional.ofNullable(serviceRequestDto.getHmctsOrgId()), headers);
        LOG.info("Ref data from service request orgunit---- {} ",organisationalServiceDto.getOrgUnit());
        LOG.info("Ref data from service request Description---- {} ",organisationalServiceDto.getServiceDescription());
        LOG.info("Ref data from service request service---- {} ",organisationalServiceDto.getServiceId());
        LOG.info("Ref data from service request ---- {} ",organisationalServiceDto.getServiceCode());
        LOG.info("Ref data from service request ---- {} ",organisationalServiceDto.getCcdServiceName());
        ServiceRequestBo serviceRequestDomain = serviceRequestDtoDomainMapper.toDomain(serviceRequestDto, organisationalServiceDto);
        return serviceRequestBo.createServiceRequest(serviceRequestDomain);

    }

    @Override
    public OnlineCardPaymentResponse create(OnlineCardPaymentRequest onlineCardPaymentRequest, String serviceRequestReference, String returnURL, String serviceCallbackURL) throws CheckDigitException {
        //find service request
        PaymentFeeLink serviceRequest = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference).orElseThrow(() -> new ServiceRequestReferenceNotFoundException("Order reference doesn't exist"));

        LOG.info("returnURL {}",returnURL);

        //General business validation
        businessValidationForOnlinePaymentServiceRequestOrder(serviceRequest, onlineCardPaymentRequest);

        //If exist, will cancel existing payment channel session with gov pay
        checkOnlinePaymentAlreadyExistWithCreatedState(serviceRequest);

        //Payment - Boundary Object
        ServiceRequestOnlinePaymentBo requestOnlinePaymentBo = serviceRequestDtoDomainMapper.toDomain(onlineCardPaymentRequest, returnURL, serviceCallbackURL);

        // GovPay - Request and creation
        CreatePaymentRequest createGovPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(requestOnlinePaymentBo);
        LOG.info("Reaching card payment");
        GovPayPayment govPayPayment = delegateGovPay.create(createGovPayRequest, serviceRequest.getEnterpriseServiceName());
        if(govPayPayment != null){
            if(!(govPayPayment.getAmount().toString().equals(onlineCardPaymentRequest.getAmount().toString()))){
                govPayPayment.setAmount(onlineCardPaymentRequest.getAmount().intValue());
            }
        }

        //Payment - Entity creation
        Payment paymentEntity = serviceRequestDomainDataEntityMapper.toPaymentEntity(requestOnlinePaymentBo, govPayPayment, serviceRequest);
        paymentEntity.setPaymentLink(serviceRequest);
        serviceRequest.getPayments().add(paymentEntity);
        paymentRepository.save(paymentEntity);

        // Trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
        LOG.info("ApportionFeature Flag Value in online card payment : {}", apportionFeature);
        if (apportionFeature) {
            //Apportion payment
            feePayApportionService.processApportion(paymentEntity);
        }

        return OnlineCardPaymentResponse.onlineCardPaymentResponseWith()
            .dateCreated(paymentEntity.getDateCreated())
            .externalReference(paymentEntity.getExternalReference())
            .nextUrl(paymentEntity.getNextUrl())
            .paymentReference(paymentEntity.getReference())
            .status(PayStatusToPayHubStatus.valueOf(paymentEntity.getPaymentStatus().getName()).getMappedStatus())
            .build();
    }

    @Override
    public ServiceRequestPaymentBo addPayments(PaymentFeeLink serviceRequest, String serviceRequestReference,
                                               ServiceRequestPaymentDto serviceRequestPaymentDto) throws CheckDigitException {

        LOG.info("PBA add payment started");
        ServiceRequestPaymentBo serviceRequestPaymentBo = serviceRequestPaymentDtoDomainMapper.toDomain(serviceRequestPaymentDto);
        serviceRequestPaymentBo.setStatus(PaymentStatus.CREATED.getName());
        Payment payment = serviceRequestPaymentDomainDataEntityMapper.toEntity(serviceRequestPaymentBo, serviceRequest);
        payment.setPaymentLink(serviceRequest);


        //2. Account check for PBA-Payment
        payment = accountCheckForPBAPayment(serviceRequest, serviceRequestPaymentDto, payment);

        List <Payment> paymentList = new ArrayList<Payment>();
        paymentList.add(payment);

        PaymentFeeLink serviceRequestWithUpdatedPaymentStatus = serviceRequest;
        serviceRequestWithUpdatedPaymentStatus.setPayments(paymentList);
        String serviceRequestStatus = paymentGroup.toPaymentGroupDto(serviceRequestWithUpdatedPaymentStatus).getServiceRequestStatus();

        PaymentStatusDto paymentStatusDto = paymentDtoMapper.toPaymentStatusDto(serviceRequestReference,
            serviceRequestPaymentBo.getAccountNumber(), payment, serviceRequestStatus);

        PaymentFeeLink serviceRequestCallbackURL = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow(() -> new ServiceRequestReferenceNotFoundException("Order reference doesn't exist"));
        sendMessageToTopic(paymentStatusDto, serviceRequestCallbackURL.getCallBackUrl());
        LOG.info("send PBA payment status to topic completed ");

        if (payment.getPaymentStatus().getName().equals(FAILED)) {
            LOG.info("CreditAccountPayment Response 402(FORBIDDEN) for ccdCaseNumber : {} PaymentStatus : {}", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
            serviceRequestPaymentBo = serviceRequestPaymentDomainDataEntityMapper.toDomain(payment);
            return serviceRequestPaymentBo;
        }

        // 3. Auto-Apportionment of Payment against serviceRequest Fees
        extractApportionmentForPBA(serviceRequest);

        LOG.info("PBA add payment completed");

        serviceRequestPaymentBo = serviceRequestPaymentDomainDataEntityMapper.toDomain(payment);
        return serviceRequestPaymentBo;
    }


    private void extractApportionmentForPBA(PaymentFeeLink serviceRequest) {
        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
        LOG.info("ApportionFeature Flag Value in CreditAccountPaymentController : {}", apportionFeature);
        if (apportionFeature) {
            //get first successful payment
            Payment pbaPayment = getFirstSuccessPayment.apply(serviceRequest);
            pbaPayment.setPaymentLink(serviceRequest);
            feePayApportionService.processApportion(pbaPayment);

            // Update Fee Amount Due as Payment Status received from PBA Payment as SUCCESS
            if (Lists.newArrayList("success", "pending").contains(pbaPayment.getPaymentStatus().getName().toLowerCase())) {
                LOG.info("Update Fee Amount Due as Payment Status received from PBA Payment as %s" + pbaPayment.getPaymentStatus().getName());
                feePayApportionService.updateFeeAmountDue(pbaPayment);
            }
        }
    }

    private Payment accountCheckForPBAPayment(PaymentFeeLink serviceRequest, ServiceRequestPaymentDto serviceRequestPaymentDto, Payment payment) {
        LOG.info("PBA Old Config Service Names : {}", pbaConfig1ServiceNames);
        Boolean isPBAConfig1Journey = pbaConfig1ServiceNames.contains(serviceRequest.getEnterpriseServiceName());

        if (!isPBAConfig1Journey) {
            LOG.info("Checking with Liberata for Service : {}", serviceRequest.getEnterpriseServiceName());
            AccountDto accountDetails;
            try {
                accountDetails = accountService.retrieve(serviceRequestPaymentDto.getAccountNumber());
                LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {}", payment.getCcdCaseNumber(), accountDetails.getStatus());
            } catch (HttpClientErrorException ex) {
                LOG.error("Account information could not be found, exception: {}", ex.getMessage());
                throw new AccountNotFoundException("Account information could not be found");
            } catch (HystrixRuntimeException hystrixRuntimeException) {
                LOG.error("Liberata response not received in time, exception: {}", hystrixRuntimeException.getMessage());
                throw new LiberataServiceTimeoutException("Unable to retrieve account information due to timeout");
            } catch (Exception ex) {
                LOG.error("Unable to retrieve account information, exception: {}", ex.getMessage());
                throw new AccountServiceUnavailableException("Unable to retrieve account information, please try again later");
            }

            pbaStatusErrorMapper.setServiceRequestPaymentStatus(serviceRequestPaymentDto.getAmount(), payment, accountDetails);
        } else {
            LOG.info("Setting status to pending");
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("pending").build());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} PaymentStatus : {} - Account Balance Sufficient!!!", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
        }

        //status history from created -> success
        if (payment.getPaymentStatus().getName().equalsIgnoreCase(SUCCESS)) {
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .build()));
        }

        //save the payment in paymentFeeLink
        serviceRequest.getPayments().add(payment);
        paymentFeeLinkRepository.save(serviceRequest);

        //Last Payment added in serviceRequest
        return serviceRequest.getPayments().get(serviceRequest.getPayments().size() - 1);
    }

    public PaymentFeeLink businessValidationForServiceRequests(PaymentFeeLink serviceRequest, ServiceRequestPaymentDto serviceRequestPaymentDto) {
        //Business validation for amount
        Optional<BigDecimal> totalCalculatedAmount = serviceRequest.getFees().stream().map(paymentFee -> paymentFee.getCalculatedAmount()).reduce(BigDecimal::add);
        if (totalCalculatedAmount.isPresent() && (totalCalculatedAmount.get().compareTo(serviceRequestPaymentDto.getAmount()) != 0)) {
            throw new ServiceRequestExceptionForNoMatchingAmount("The amount should be equal to serviceRequest balance");
        }


        //Business validation for amount due for fees
        Optional<BigDecimal> totalAmountDue = serviceRequest.getFees().stream().map(paymentFee -> paymentFee.getAmountDue()).reduce(BigDecimal::add);
        if (totalAmountDue.isPresent() && totalAmountDue.get().compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceRequestExceptionForNoAmountDue("The serviceRequest has already been paid");
        }

        return serviceRequest;
    }

    private void businessValidationForOnlinePaymentServiceRequestOrder(PaymentFeeLink order, OnlineCardPaymentRequest request) {

        //Business validation for amount
        Optional<BigDecimal> totalCalculatedAmount = order.getFees().stream().map(paymentFee -> paymentFee.getCalculatedAmount()).reduce(BigDecimal::add);
        if (totalCalculatedAmount.isPresent() && (totalCalculatedAmount.get().compareTo(request.getAmount()) != 0)) {
            throw new ServiceRequestExceptionForNoMatchingAmount("The amount should be equal to serviceRequest balance");
        }

        //Business validation for amount due for fees
        Optional<BigDecimal> totalAmountDue = order.getFees().stream().map(paymentFee -> paymentFee.getAmountDue()).reduce(BigDecimal::add);
        if (totalAmountDue.isPresent() && totalAmountDue.get().compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceRequestExceptionForNoAmountDue("The serviceRequest has already been paid");
        }
    }

    private void checkOnlinePaymentAlreadyExistWithCreatedState(PaymentFeeLink paymentFeeLink)  {
        //Already created state payment existed, then cancel gov pay section present
        Date ninetyMinAgo = new Date(System.currentTimeMillis() - 90 * 60 * 1000);
        Optional<Payment> existedPayment = paymentFeeLink.getPayments().stream()
            .filter(payment -> payment.getPaymentStatus().getName().equalsIgnoreCase("created")
                && payment.getPaymentProvider().getName().equalsIgnoreCase("gov pay")
                && payment.getDateCreated().compareTo(ninetyMinAgo) >= 0)
            .sorted(Comparator.comparing(Payment::getDateCreated).reversed())
            .findFirst();

        if (!existedPayment.isEmpty()) {
            delegatingPaymentService.cancel(existedPayment.get(), paymentFeeLink.getCcdCaseNumber(),paymentFeeLink.getEnterpriseServiceName());
        }
    }

    public ResponseEntity createIdempotencyRecord(ObjectMapper objectMapper, String idempotencyKey, String serviceRequestReference,
                                                  String responseJson, ResponseEntity<?> responseEntity, ServiceRequestPaymentDto serviceRequestPaymentDto) throws JsonProcessingException {
        String requestJson = objectMapper.writeValueAsString(serviceRequestPaymentDto);
        int requestHashCode = serviceRequestPaymentDto.hashCodeWithServiceRequestReference(serviceRequestReference);

        IdempotencyKeys idempotencyRecord = IdempotencyKeys
            .idempotencyKeysWith()
            .idempotencyKey(idempotencyKey)
            .requestBody(requestJson)
            .request_hashcode(requestHashCode)   //save the hashcode
            .responseBody(responseJson)
            .responseCode(responseEntity.getStatusCodeValue())
            .build();

        try {
            Optional<IdempotencyKeys> idempotencyKeysRecord = idempotencyKeysRepository.findById(IdempotencyKeysPK.idempotencyKeysPKWith().idempotencyKey(idempotencyKey).request_hashcode(requestHashCode).build());
            if (idempotencyKeysRecord.isPresent()) {
                return new ResponseEntity<>(objectMapper.readValue(idempotencyKeysRecord.get().getResponseBody(), ServiceRequestPaymentBo.class), HttpStatus.valueOf(idempotencyKeysRecord.get().getResponseCode()));
            }
            idempotencyKeysRepository.save(idempotencyRecord);

        } catch (DataIntegrityViolationException exception) {
            responseEntity = new ResponseEntity<>("Too many requests.PBA Payment currently is in progress for this serviceRequest", HttpStatus.TOO_EARLY);
        }

        return responseEntity;
    }

    @Override
    public Boolean isDuplicate(String serviceRequestReference) {
        return Optional.of(find(serviceRequestReference)).isPresent();
    }

    @Override

    public IMessageReceiver createDLQConnection() throws ServiceBusException, InterruptedException {

        String subName = "serviceRequestCpoUpdateSubscription";
        String topic = "ccpay-service-request-cpo-update-topic";
        IMessageReceiver subscriptionClient = ClientFactory.createMessageReceiverFromConnectionStringBuilder(new ConnectionStringBuilder(connectionString, topic+"/subscriptions/" + subName+"/$deadletterqueue"));
        return subscriptionClient;
    }


    @Override

    public void deadLetterProcess(IMessageReceiver subscriptionClient) throws ServiceBusException, InterruptedException, IOException {


        int receivedMessages =0;

        TopicClientProxy topicClientCPO = topicClientService.getTopicClientProxy();
        LOG.info("topicClientCPO : " + topicClientCPO );
        while (true)
        {
            IMessage receivedMessage = subscriptionClient.receive();
            LOG.info("receivedMessage: {}", receivedMessage);
            if (receivedMessage != null) {
                String  msgProperties = receivedMessage.getProperties().toString();
                LOG.info("Dead letter process, msg properties: {}", msgProperties);
                boolean isFound500 =  msgProperties.indexOf("500") !=-1? true: false;
                LOG.info("isFound500: {},", isFound500);
                if (isFound500) {
                    byte[] body = receivedMessage.getBody();
                    ObjectMapper objectMapper = new ObjectMapper();
                    DeadLetterDto deadLetterDto = objectMapper.readValue(body, DeadLetterDto.class);
                    ObjectMapper objectMapper1 = new ObjectMapper();
                    Message msg = new Message(objectMapper1.writeValueAsString(deadLetterDto));
                    msg.setContentType(MSGCONTENTTYPE);
                    LOG.info("Dead letter process, message being sent: {}", msg);
                    LOG.info("Dead letter process, message properties.keySet: {}", msg.getProperties().keySet());
                    LOG.info("Dead letter process, message properties.toString: {}", msg.getProperties().toString());
                    LOG.info("Dead letter process, message.getBody  {}", msg.getBody());
                    LOG.info("Dead letter process, message.getDeadLetterSource: {}", msg.getDeadLetterSource());
                    topicClientCPO.send(msg);
                }
            }
            else
            {
                topicClientCPO.close();
                subscriptionClient.close();
                break;
            }
        }
        LOG.info("Received %s messages from subscription.\n", receivedMessages);
    }

    @Override
    public void sendMessageTopicCPO(ServiceRequestDto serviceRequestDto, String serviceRequestReference){

        try {
            TopicClientProxy topicClientCPO = null;
            Message msg = null;
            ObjectMapper objectMapper = new ObjectMapper();

            LOG.info("Connection String: {}", connectionString);

            ServiceRequestCpoDto serviceRequestCpoDto = ServiceRequestCpoDto.serviceRequestCpoDtoWith()
                    .action(serviceRequestDto.getCasePaymentRequest().getAction())
                    .case_id(serviceRequestDto.getCcdCaseNumber())
                    .order_reference(serviceRequestReference)
                    .responsible_party(serviceRequestDto.getCasePaymentRequest().getResponsibleParty())
                    .build();

            msg = new Message(objectMapper.writeValueAsString(serviceRequestCpoDto));

            topicClientCPO = new TopicClientProxy(connectionString, topic);
            LOG.info("sending message started..");
            LOG.info("Message sent: {}", msg);
            LOG.info("message content Action: {}",serviceRequestCpoDto.getAction() );
            LOG.info("message content case id: {}",serviceRequestCpoDto.getCase_id() );
            LOG.info("message content order reference: {}",serviceRequestCpoDto.getOrder_reference() );
            LOG.info("message content res party: {}",serviceRequestCpoDto.getResponsible_party() );

            if(msg!=null && topicClientCPO!=null){
                msg.setContentType(MSGCONTENTTYPE);
                msg.setLabel("Service Callback Message");
                msg.setProperties(Collections.singletonMap("serviceCallbackUrl",
                    callBackUrl+"/case-payment-orders"));
                topicClientCPO.send(msg);
                topicClientCPO.close();
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendMessageToTopic(PaymentStatusDto payment, String callBackUrl){
        try {
            TopicClientProxy topicClientCPO = null;
            Message msg = null;
            ObjectMapper objectMapper = new ObjectMapper();

            LOG.info("Callback URL: {}", callBackUrl);

            if(payment!=null){
                LOG.info("Connection String CardPBA: {}", connectionString);
                msg = new Message(objectMapper.writeValueAsString(payment));
                topicClientCPO = new TopicClientProxy(connectionString, topicCardPBA);
                msg.setContentType(MSGCONTENTTYPE);
                msg.setLabel("Service Callback Message");
                msg.setProperties(Collections.singletonMap("serviceCallbackUrl",callBackUrl));
                LOG.info("Message being sent to topic: {}", msg);
                topicClientCPO.send(msg);
                topicClientCPO.close();
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}
