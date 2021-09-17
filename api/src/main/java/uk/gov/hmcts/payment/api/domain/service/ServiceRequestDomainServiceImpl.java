package uk.gov.hmcts.payment.api.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.OrderPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.IdempotencyKeysPK;
import uk.gov.hmcts.payment.api.model.IdempotencyKeysRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.OrderExceptionForNoAmountDue;
import uk.gov.hmcts.payment.api.v1.model.exceptions.OrderExceptionForNoMatchingAmount;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentGroupNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoAmountDue;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoMatchingAmount;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServiceRequestDomainServiceImpl implements ServiceRequestDomainService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRequestDomainServiceImpl.class);
    private static final String FAILED = "failed";
    private static final String SUCCESS = "success";

    @Autowired
    private ServiceRequestDtoDomainMapper serviceRequestDtoDomainMapper;

    @Autowired
    private ServiceRequestDomainDataEntityMapper serviceRequestDomainDataEntityMapper;

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

    private Function<PaymentFeeLink, Payment> getFirstSuccessPayment = order -> order.getPayments().stream().
        filter(payment -> payment.getPaymentStatus().getName().equalsIgnoreCase("success")).collect(Collectors.toList()).get(0);

    @Override
    public List<PaymentFeeLink> findByCcdCaseNumber(String ccdCaseNumber) {
        Optional<List<PaymentFeeLink>> paymentFeeLinks = paymentFeeLinkRepository.findByCcdCaseNumber(ccdCaseNumber);
        return paymentFeeLinks.orElseThrow(() -> new PaymentGroupNotFoundException("Order detail not found for given ccdcasenumber " + ccdCaseNumber));
    }

    @Override
    public PaymentFeeLink find(String serviceRequestReference) {
        return (PaymentFeeLink) paymentGroupService.findByPaymentGroupReference(serviceRequestReference);
    }

    @Override
    @Transactional
    public ServiceRequestResponseDto create(ServiceRequestDto serviceRequestDto, MultiValueMap<String, String> headers) {

        OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(Optional.empty(), Optional.ofNullable(serviceRequestDto.getHmctsOrgId()), headers);

        ServiceRequestBo serviceRequestDomain = serviceRequestDtoDomainMapper.toDomain(serviceRequestDto, organisationalServiceDto);
        return serviceRequestBo.createServiceRequest(serviceRequestDomain);

    }

    @Override
    public OnlineCardPaymentResponse create(OnlineCardPaymentRequest onlineCardPaymentRequest, String serviceRequestReference, String returnURL, String serviceCallbackURL) throws CheckDigitException {
        //find service request
        PaymentFeeLink serviceRequestOrder = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference).orElseThrow(() -> new ServiceRequestReferenceNotFoundException("Order reference doesn't exist"));


        //General business validation
        businessValidationForOnlinePaymentServiceRequestOrder(serviceRequestOrder, onlineCardPaymentRequest);

        //If exist, will cancel existing payment channel session with gov pay
        checkOnlinePaymentAlreadyExistWithCreatedState(serviceRequestOrder);

        //Payment - Boundary Object
        ServiceRequestOnlinePaymentBo requestOnlinePaymentBo = serviceRequestDtoDomainMapper.toDomain(onlineCardPaymentRequest, returnURL, serviceCallbackURL);

        // GovPay - Request and creation
        CreatePaymentRequest createGovPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(requestOnlinePaymentBo);
        GovPayPayment govPayPayment = delegateGovPay.create(createGovPayRequest);

        //Payment - Entity creation
        Payment paymentEntity = serviceRequestDomainDataEntityMapper.toPaymentEntity(requestOnlinePaymentBo, govPayPayment);
        paymentEntity.setPaymentLink(serviceRequestOrder);
        serviceRequestOrder.getPayments().add(paymentEntity);
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
            .status(paymentEntity.getPaymentStatus().getName())
            .build();
    }

    @Override
    public OrderPaymentBo addPayments(PaymentFeeLink order, OrderPaymentDto orderPaymentDto) throws CheckDigitException {

        OrderPaymentBo orderPaymentBo = orderPaymentDtoDomainMapper.toDomain(orderPaymentDto);
        orderPaymentBo.setStatus(PaymentStatus.CREATED.getName());
        Payment payment = orderPaymentDomainDataEntityMapper.toEntity(orderPaymentBo, order);
        payment.setPaymentLink(order);

        //2. Account check for PBA-Payment
        payment = accountCheckForPBAPayment(order, orderPaymentDto, payment);

        if (payment.getPaymentStatus().getName().equals(FAILED)) {
            LOG.info("CreditAccountPayment Response 402(FORBIDDEN) for ccdCaseNumber : {} PaymentStatus : {}", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
            orderPaymentBo = orderPaymentDomainDataEntityMapper.toDomain(payment);
            return orderPaymentBo;
        }

        // 3. Auto-Apportionment of Payment against Order Fees
        extractApportionmentForPBA(order);

        orderPaymentBo = orderPaymentDomainDataEntityMapper.toDomain(payment);
        return orderPaymentBo;
    }


    private void extractApportionmentForPBA(PaymentFeeLink order) {
        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
        LOG.info("ApportionFeature Flag Value in CreditAccountPaymentController : {}", apportionFeature);
        if (apportionFeature) {
            //get first successful payment
            Payment pbaPayment = getFirstSuccessPayment.apply(order);
            pbaPayment.setPaymentLink(order);
            feePayApportionService.processApportion(pbaPayment);

            // Update Fee Amount Due as Payment Status received from PBA Payment as SUCCESS
            if (Lists.newArrayList("success", "pending").contains(pbaPayment.getPaymentStatus().getName().toLowerCase())) {
                LOG.info("Update Fee Amount Due as Payment Status received from PBA Payment as %s" + pbaPayment.getPaymentStatus().getName());
                feePayApportionService.updateFeeAmountDue(pbaPayment);
            }
        }
    }

    private Payment accountCheckForPBAPayment(PaymentFeeLink order, OrderPaymentDto orderPaymentDto, Payment payment) {
        LOG.info("PBA Old Config Service Names : {}", pbaConfig1ServiceNames);
        Boolean isPBAConfig1Journey = pbaConfig1ServiceNames.contains(order.getEnterpriseServiceName());

        if (!isPBAConfig1Journey) {
            LOG.info("Checking with Liberata for Service : {}", order.getEnterpriseServiceName());
            AccountDto accountDetails;
            try {
                accountDetails = accountService.retrieve(orderPaymentDto.getAccountNumber());
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

            pbaStatusErrorMapper.setOrderPaymentStatus(orderPaymentDto.getAmount(), payment, accountDetails);
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
        order.getPayments().add(payment);
        paymentFeeLinkRepository.save(order);

        //Last Payment added in order
        return order.getPayments().get(order.getPayments().size() - 1);
    }

    public PaymentFeeLink businessValidationForOrders(PaymentFeeLink order, OrderPaymentDto orderPaymentDto) {
        //Business validation for amount
        Optional<BigDecimal> totalCalculatedAmount = order.getFees().stream().map(paymentFee -> paymentFee.getCalculatedAmount()).reduce(BigDecimal::add);
        if (totalCalculatedAmount.isPresent() && (totalCalculatedAmount.get().compareTo(orderPaymentDto.getAmount()) != 0)) {
            throw new OrderExceptionForNoMatchingAmount("The payment amount should be equal to order balance");
        }


        //Business validation for amount due for fees
        Optional<BigDecimal> totalAmountDue = order.getFees().stream().map(paymentFee -> paymentFee.getAmountDue()).reduce(BigDecimal::add);
        if (totalAmountDue.isPresent() && totalAmountDue.get().compareTo(BigDecimal.ZERO) == 0) {
            throw new OrderExceptionForNoAmountDue("The service request has already been paid");
        }

        return order;
    }

    private void businessValidationForOnlinePaymentServiceRequestOrder(PaymentFeeLink order, OnlineCardPaymentRequest request) {

        //Business validation for amount
        Optional<BigDecimal> totalCalculatedAmount = order.getFees().stream().map(paymentFee -> paymentFee.getCalculatedAmount()).reduce(BigDecimal::add);
        if (totalCalculatedAmount.isPresent() && (totalCalculatedAmount.get().compareTo(request.getAmount()) != 0)) {
            throw new ServiceRequestExceptionForNoMatchingAmount("The payment amount should be equal to order balance");
        }

        //Business validation for amount due for fees
        Optional<BigDecimal> totalAmountDue = order.getFees().stream().map(paymentFee -> paymentFee.getAmountDue()).reduce(BigDecimal::add);
        if (totalAmountDue.isPresent() && totalAmountDue.get().compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceRequestExceptionForNoAmountDue("The order has already been paid");
        }
    }

    private void checkOnlinePaymentAlreadyExistWithCreatedState(PaymentFeeLink paymentFeeLink) {
        //Already created state payment existed, then cancel gov pay section present
        Date ninetyMinAgo = new Date(System.currentTimeMillis() - 90 * 60 * 1000);
        Optional<Payment> existedPayment = paymentFeeLink.getPayments().stream()
            .filter(payment -> payment.getPaymentStatus().getName().equalsIgnoreCase("created")
                && payment.getPaymentProvider().getName().equalsIgnoreCase("gov pay")
                && payment.getDateCreated().compareTo(ninetyMinAgo) >= 0)
            .sorted(Comparator.comparing(Payment::getDateCreated).reversed())
            .findFirst();

        if (!existedPayment.isEmpty()) {
            delegatingPaymentService.cancel(existedPayment.get(), paymentFeeLink.getCcdCaseNumber());
        }
    }

    public ResponseEntity createIdempotencyRecord(ObjectMapper objectMapper, String idempotencyKey, String orderReference,
                                                  String responseJson, ResponseEntity<?> responseEntity, OrderPaymentDto orderPaymentDto) throws JsonProcessingException {
        String requestJson = objectMapper.writeValueAsString(orderPaymentDto);
        int requestHashCode = orderPaymentDto.hashCodeWithOrderReference(orderReference);

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
                return new ResponseEntity<>(objectMapper.readValue(idempotencyKeysRecord.get().getResponseBody(), OrderPaymentBo.class), HttpStatus.valueOf(idempotencyKeysRecord.get().getResponseCode()));
            }
            idempotencyKeysRepository.save(idempotencyRecord);

        } catch (DataIntegrityViolationException exception) {
            responseEntity = new ResponseEntity<>("Too many requests.PBA Payment currently is in progress for this order", HttpStatus.TOO_EARLY);
        }

        return responseEntity;
    }

    @Override
    public Boolean isDuplicate(String orderReference) {
        return Optional.of(find(orderReference)).isPresent();
    }
}
