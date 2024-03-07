package uk.gov.hmcts.payment.api.service;

import java.math.BigInteger;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.EnumUtils;
import org.apache.poi.hpsf.Decimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.RefundsFeeDto;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.exception.InvalidPartialRefundRequestException;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.RefundEligibilityUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PaymentRefundsServiceImpl implements PaymentRefundsService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentRefundsServiceImpl.class);
    private static final String REFUND_ENDPOINT = "/refund";
    private static final String AUTHORISED_REFUNDS_ROLE = "payments-refund";
    private static final String AUTHORISED_REFUNDS_APPROVER_ROLE = "payments-refund-approver";
    private static final Pattern EMAIL_ID_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    final Predicate<Payment> paymentSuccessCheck =
        payment -> payment.getPaymentStatus().getName().equals(PaymentStatus.SUCCESS.getName());

    @Autowired
    RemissionRepository remissionRepository;

    @Autowired
    FeePayApportionRepository feePayApportionRepository;

    @Autowired
    private Payment2Repository paymentRepository;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired()
    @Qualifier("restTemplateRefundsGroup")
    private RestTemplate restTemplateRefundsGroup;
    @Value("${refund.api.url}")
    private String refundApiUrl;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;
    @Autowired
    private IdamService idamService;
    @Autowired
    private RefundRemissionEnableService refundRemissionEnableService;
    @Autowired
    private RefundEligibilityUtil refundEligibilityUtil;


    @Autowired
    PaymentFailureRepository paymentFailureRepository;

    public ResponseEntity<RefundResponse> createRefund(PaymentRefundRequest paymentRefundRequest, MultiValueMap<String, String> headers) {

        validateContactDetails(paymentRefundRequest.getContactDetails());

        Payment payment = paymentRepository.findByReference(paymentRefundRequest.getPaymentReference()).orElseThrow(PaymentNotFoundException::new);

        validateRefund(paymentRefundRequest,payment.getPaymentLink().getFees(), payment);

        validateThePaymentBeforeInitiatingRefund(payment,headers);

        RefundRequestDto refundRequest = RefundRequestDto.refundRequestDtoWith()
            .paymentReference(paymentRefundRequest.getPaymentReference())
            .refundAmount(paymentRefundRequest.getTotalRefundAmount())
            .paymentAmount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod().getName())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .refundReason(paymentRefundRequest.getRefundReason())
            .feeIds(getFeeIds(paymentRefundRequest.getFees()))
            .refundFees(getRefundFees(paymentRefundRequest.getFees()))
            .contactDetails(paymentRefundRequest.getContactDetails())
            .serviceType(payment.getServiceType())
            .paymentChannel(payment.getPaymentChannel().getName())
            .build();

        RefundResponse refundResponse = RefundResponse.RefundResponseWith()
            .refundAmount(paymentRefundRequest.getTotalRefundAmount())
            .refundReference(postToRefundService(refundRequest, headers)).build();
        LOG.info("After return from Refunds service in createRefund PaymentRefundsServiceImpl");
        return new ResponseEntity<>(refundResponse, HttpStatus.CREATED);

    }

    private void validateContactDetails(ContactDetails contactDetails) {
        Matcher matcher = null;
        if (null != contactDetails && null != contactDetails.getEmail())
            matcher = EMAIL_ID_REGEX.matcher(contactDetails.getEmail());
        if (null == contactDetails ||
                contactDetails.toString().equals("{}")) {
            throw new InvalidRefundRequestException("Contact Details should not be null or empty");
        } else if (null == contactDetails.getNotificationType() ||
                contactDetails.getNotificationType().isEmpty()) {
            throw new InvalidRefundRequestException("Notification Type should not be null or empty");
        } else if (!EnumUtils
                .isValidEnum(Notification.class, contactDetails.getNotificationType())) {
            throw new InvalidRefundRequestException("Notification Type should be EMAIL or LETTER");
        } else if (Notification.EMAIL.getNotification()
                .equals(contactDetails.getNotificationType())
                && (null == contactDetails.getEmail() ||
                contactDetails.getEmail().isEmpty())) {
            throw new InvalidRefundRequestException("Email id should not be null or empty");
        } else if (Notification.LETTER.getNotification()
                .equals(contactDetails.getNotificationType())
                && (null == contactDetails.getPostalCode() ||
                contactDetails.getPostalCode().isEmpty())) {
            throw new InvalidRefundRequestException("Postal code should not be null or empty");
        } else if (Notification.EMAIL.getNotification()
                .equals(contactDetails.getNotificationType())
                && null != matcher && !matcher.find()) {
            throw new InvalidRefundRequestException("Email id is not valid");
        }
    }

    @Override
    public ResponseEntity<RefundResponse> createAndValidateRetrospectiveRemissionRequest(
            RetrospectiveRemissionRequest retrospectiveRemissionRequest, MultiValueMap<String, String> headers) {

        validateContactDetails(retrospectiveRemissionRequest.getContactDetails());

        Optional<Remission> remission = remissionRepository.findByRemissionReference(retrospectiveRemissionRequest.getRemissionReference());
        PaymentFee paymentFee;
        Integer paymentId;

        if (remission.isPresent()) {
            //remissionAmount
            paymentFee = remission.get().getFee();
            //need to validate if multipleApportionment scenario present for single feeId validation needed
            Optional<List<FeePayApportion>> feePayApportion = feePayApportionRepository.findByFeeId(paymentFee.getId());
            if (feePayApportion.isPresent() && !feePayApportion.get().isEmpty()) {
                Optional<FeePayApportion> result = feePayApportion.get().stream().findFirst();
                paymentId = result.get().getPaymentId();

                Payment payment = paymentRepository
                    .findById(paymentId).orElseThrow(() -> new PaymentNotFoundException("Payment not found for given apportionment"));

                BigDecimal remissionAmount =  getRefundAmount(payment,remission.get());
                validateThePaymentBeforeInitiatingRefund(payment, headers);

                RefundRequestDto refundRequest = RefundRequestDto.refundRequestDtoWith()
                    .paymentReference(payment.getReference()) //RC reference
                    .refundAmount(remissionAmount) //Refund amount
                    .paymentAmount(payment.getAmount())
                    .ccdCaseNumber(payment.getCcdCaseNumber()) // ccd case number
                    .refundReason("RR036")//Refund reason category would be other
                    .feeIds(getFeeIdsUsingPaymentFees(Collections.singletonList(paymentFee)))
                    .refundFees(getRefundFeesUsingPaymentFee(Collections.singletonList(paymentFee),remissionAmount ))
                    .serviceType(payment.getServiceType())
                    .paymentMethod(payment.getPaymentMethod().getName())
                    .contactDetails(retrospectiveRemissionRequest.getContactDetails())
                    .paymentChannel(payment.getPaymentChannel().getName())
                    .build();
                RefundResponse refundResponse = RefundResponse.RefundResponseWith()
                    .refundAmount(remissionAmount)
                    .refundReference(postToRefundService(refundRequest, headers)).build();
                return new ResponseEntity<>(refundResponse, HttpStatus.CREATED);
            }else{
                throw new PaymentNotSuccessException("Refund can be possible if payment is successful");
            }

        }

        throw new RemissionNotFoundException("Remission not found for given remission reference");
    }


    private BigDecimal getRefundAmount(Payment payment,Remission remission){

        if ( payment.getPaymentLink().getFees().size() > 1 ){
            LOG.info("The payment has more than one fee, The refund value to used is getHwfAmount {}",remission.getHwfAmount());
            return remission.getHwfAmount();
        }

        if (remission.getFee() == null || remission.getFee().getRemissions() == null ||  remission.getFee().getRemissions().isEmpty()) {
            LOG.info("No Remission found for the fee, The refund value to used is getHwfAmount {}",remission.getHwfAmount());
            return remission.getHwfAmount();
        }
        BigDecimal remissionAmount = payment.getAmount().subtract(
            remission.getFee().getCalculatedAmount().subtract(
                remission.getFee().getRemissions().get(0).getHwfAmount()
                    )
                );
        if (remissionAmount.compareTo(BigDecimal.ZERO) > 0){
            LOG.info("Calculated refund to be sent to refund service {}",remissionAmount);
            return remissionAmount;
        }
        LOG.info("Refund to be sent to refund service is getHwfAmount {}",remission.getHwfAmount());
        return remission.getHwfAmount();
    }

  @Override
    public ResponseEntity updateTheRemissionAmount(String paymentReference, ResubmitRefundRemissionRequest request) {
        //Payment not found exception
        Payment payment = paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);

            if (payment.getAmount().compareTo(request.getTotalRefundedAmount()) < 0) {
                throw new InvalidRefundRequestException("Refund amount should not be more than Payment amount");
            }

            //If refund reason is retro-remission
            if (request.getRefundReason().contains("RR036")) {
                    Integer feeId = Integer.parseInt(request.getFeeId());
                    updateRemissionAmount(feeId, request.getAmount());
            }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
    private RefundListDtoResponse getRefundsFromRefundService(String ccdCaseNumber, MultiValueMap<String, String> headers) {


        RefundListDtoResponse refundListDtoResponse;

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refundApiUrl + REFUND_ENDPOINT).queryParam("ccdCaseNumber",ccdCaseNumber);

        LOG.info("builder.toUriString() : {}", builder.toUriString());

        try {

            LOG.info("restTemplateRefundsGroup : {}", restTemplateRefundsGroup);

            // call refund app
            ResponseEntity<RefundListDtoResponse> refundListDtoResponseEntity  = restTemplateRefundsGroup
                .exchange(builder.toUriString(), HttpMethod.GET, createEntity(headers), RefundListDtoResponse.class);

            refundListDtoResponse = refundListDtoResponseEntity.hasBody() ? refundListDtoResponseEntity.getBody() : null;

        } catch (HttpClientErrorException e) {

            LOG.error("client err ", e);

            refundListDtoResponse = null;

        }

        return refundListDtoResponse;

    }

    private HttpEntity<HttpHeaders> createEntity(MultiValueMap<String, String> headers) {

        MultiValueMap<String, String> headerMultiValueMap = new LinkedMultiValueMap<String, String>();

        String serviceAuthorisation = authTokenGenerator.generate();

        headerMultiValueMap.put("Content-Type", headers.get("content-type"));

        String userAuthorization = headers.get("authorization") != null ? headers.get("authorization").get(0) : headers.get("Authorization").get(0);

        headerMultiValueMap.put("Authorization", Collections.singletonList(userAuthorization.startsWith("Bearer ")
            ? userAuthorization : "Bearer ".concat(userAuthorization)));

        headerMultiValueMap.put("ServiceAuthorization", Collections.singletonList(serviceAuthorisation));

        HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMap);

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(httpHeaders);
    }

    public void updateRemissionAmount(Integer feeId, BigDecimal remissionAmount) {
//        if (feeId != null) {
            //Remission against fee
            Optional<Remission> remission = remissionRepository.findByFeeId(feeId);

            if (remission.isPresent()) {
                if (remission.get().getFee().getCalculatedAmount().compareTo(remissionAmount) < 0) {
                    throw new InvalidRefundRequestException("Remission Amount should not be more than Fee amount");
                } else {
                    //update remissionAmount
                    remission.get().setHwfAmount(remissionAmount);
                    remissionRepository.save(remission.get());
                }
            }

//        }
    }

    public static boolean isEmptyOrNull(Collection< ? > collection) {
        return (collection == null || collection.isEmpty());
    }

    private void validateThePaymentBeforeInitiatingRefund(Payment payment,MultiValueMap<String, String> headers) {

        Optional<List<PaymentFailures>> paymentFailuresList;
        paymentFailuresList = paymentFailureRepository.findByPaymentReference(payment.getReference());

        if(paymentFailuresList.isPresent() && !paymentFailuresList.get().isEmpty()){
            boolean match = paymentFailuresList.get().stream().anyMatch(paymentFailuresList1 -> paymentFailuresList1.getRepresentmentSuccess() == null || paymentFailuresList1.getRepresentmentSuccess().equalsIgnoreCase("no"));
           if(match) {
               throw new PaymentNotSuccessException("Refund can't be requested for failed payment");
           }
        }

        //payment success check
        if (!paymentSuccessCheck.test(payment)) {
            throw new PaymentNotSuccessException("Refund can not be processed for unsuccessful payment");
        }

        boolean refundLagTimefeature = featureToggler.getBooleanValue("refund-remission-lagtime-feature",false);

        LOG.info("RefundEnableFeature Flag Value in PaymentRefundsServiceImpl : {}", refundLagTimefeature);

        if(refundLagTimefeature){

            long timeDuration= ChronoUnit.HOURS.between( payment.getDateUpdated().toInstant(), new Date().toInstant());
            boolean isRefundPermit=refundEligibilityUtil.getRefundEligiblityStatus(payment,timeDuration);

            if (!isRefundPermit) {
                throw new InvalidRefundRequestException("This payment is not yet eligible for refund");
            }
        }
    }


    private String postToRefundService(RefundRequestDto refundRequest, MultiValueMap<String, String> headers) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refundApiUrl + REFUND_ENDPOINT);
        LOG.info("builder.toUriString() : {}", builder.toUriString());
        LOG.info("refundRequest: {}", refundRequest);
        try {
            ResponseEntity<InternalRefundResponse> refundResponseResponseEntity = restTemplateRefundsGroup
                .exchange(builder.toUriString(), HttpMethod.POST, createEntity(headers, refundRequest), InternalRefundResponse.class);
            InternalRefundResponse refundResponse = refundResponseResponseEntity.hasBody() ? refundResponseResponseEntity.getBody() : null;
            if (refundResponse == null) {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Refund couldn't initiate, Please try again later");
            } else {
                return refundResponse.getRefundReference();
            }
        } catch (HttpClientErrorException e) {
            LOG.error("client err ", e);
            throw new InvalidRefundRequestException(e.getResponseBodyAsString());
        }
    }

    private HttpEntity<RefundRequestDto> createEntity(MultiValueMap<String, String> headers, RefundRequestDto refundRequest) {
        MultiValueMap<String, String> headerMultiValueMap = new LinkedMultiValueMap<String, String>();
//        String serviceAuthorisation = " authTokenGenerator.generate()";
        String serviceAuthorisation = authTokenGenerator.generate();
        headerMultiValueMap.put("Content-Type", headers.get("content-type"));
        String userAuthorization = headers.get("authorization") != null ? headers.get("authorization").get(0) : headers.get("Authorization").get(0);
        headerMultiValueMap.put("Authorization", Collections.singletonList(userAuthorization.startsWith("Bearer ")
            ? userAuthorization : "Bearer ".concat(userAuthorization)));
        headerMultiValueMap.put("ServiceAuthorization", Collections.singletonList(serviceAuthorisation));
        HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMap);
        return new HttpEntity<>(refundRequest, httpHeaders);
    }

    private List<RefundFeesDto> getRefundFees(List<RefundsFeeDto> refundFees) {
        return refundFees.stream()
            .map(fee -> RefundFeesDto.refundFeesDtoWith()
                .fee_id(fee.getId())
                .code(fee.getCode())
                .version(fee.getVersion())
                .volume(fee.getUpdatedVolume())
                .refundAmount(fee.getRefundAmount())
                .build())
            .collect(Collectors.toList());
    }

    private List<RefundFeesDto> getRefundFeesUsingPaymentFee(List<PaymentFee> paymentFees, BigDecimal remissionAmount) {
        return paymentFees.stream()
            .map(fee -> RefundFeesDto.refundFeesDtoWith()
                .fee_id(fee.getId())
                .code(fee.getCode())
                .version(fee.getVersion())
                .volume(fee.getVolume())
                .refundAmount(remissionAmount)
                .build())
            .collect(Collectors.toList());
    }

    private String getFeeIds(List<RefundsFeeDto> refundFees) {
        return refundFees.stream()
            .map(fee -> fee.getId().toString())
            .collect(Collectors.joining(","));
    }

    private String getFeeIdsUsingPaymentFees(List<PaymentFee> paymentFees) {
        return paymentFees.stream()
            .map(fee -> fee.getId().toString())
            .collect(Collectors.joining(","));
    }

    private void validateRefund(PaymentRefundRequest paymentRefundRequest, List<PaymentFee> paymentFeeList, Payment payment) {

        if(paymentRefundRequest.getTotalRefundAmount().compareTo(BigDecimal.valueOf(0))==0)
            throw new InvalidPartialRefundRequestException("You need to enter a refund amount");

        BigDecimal totalRefundAmount = calculateRefundAmount(paymentRefundRequest, payment.getPaymentLink().getFees());

        if(totalRefundAmount.compareTo(paymentRefundRequest.getTotalRefundAmount()) != 0){

            throw new InvalidPartialRefundRequestException("Invalid request");
        }

        if(totalRefundAmount.compareTo(payment.getAmount())>0){

            throw new InvalidPartialRefundRequestException("The amount to refund can not be more than £" + payment.getAmount());
        }

        for(PaymentFee paymentFee : paymentFeeList){
            for (RefundsFeeDto feeDto : paymentRefundRequest.getFees()) {

                if (feeDto.getId().intValue() == paymentFee.getId().intValue()){

                    if(feeDto.getUpdatedVolume()==0)
                        throw new InvalidPartialRefundRequestException("You need to enter a valid number");

                    if(feeDto.getRefundAmount().compareTo(payment.getAmount())>0)
                        throw new InvalidPartialRefundRequestException("The amount you want to refund is more than the amount paid");

                    if(feeDto.getUpdatedVolume()>paymentFee.getVolume())
                        throw new InvalidPartialRefundRequestException("The quantity you want to refund is more than the available quantity");

                    LOG.info("feeDto.getRefundAmount(): {}", feeDto.getRefundAmount());
                    LOG.info("feeDto.getUpdatedVolume(): {}", feeDto.getUpdatedVolume());

                   if(paymentFee.getVolume().intValue()>1){

                       BigDecimal dividedAmount  = paymentFee.getCalculatedAmount()
                           .divide(new BigDecimal(paymentFee.getVolume()));

                       BigDecimal multipliedResult = dividedAmount.multiply(new BigDecimal(feeDto.getUpdatedVolume()));

                       if(feeDto.getRefundAmount().compareTo(multipliedResult) !=0 && !(paymentRefundRequest.isOverPayment())) {
                           LOG.info("Volume : {}", feeDto.getUpdatedVolume());
                           throw new InvalidPartialRefundRequestException("The Amount to Refund should be equal to the product of Fee Amount and quantity");
                       }
                   }

                    if(feeDto.getRefundAmount().compareTo(feeDto.getApportionAmount())==0 && feeDto.getUpdatedVolume()<paymentFee.getVolume()
                        && feeDto.getUpdatedVolume()>1)
                        throw new InvalidPartialRefundRequestException("The quantity you want to refund should be maximum in case of full refund");

                }
            }
        }
    }

    private BigDecimal calculateRefundAmount(PaymentRefundRequest paymentRefundRequest, List<PaymentFee> paymentFeeList) {

        if(paymentRefundRequest.getTotalRefundAmount().compareTo(BigDecimal.valueOf(0))==0)
            throw new InvalidPartialRefundRequestException("You need to enter a refund amount");

        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        for(PaymentFee paymentFee : paymentFeeList) {
            for (RefundsFeeDto feeDto : paymentRefundRequest.getFees()) {

                if (feeDto.getId().intValue() == paymentFee.getId().intValue()){

                    totalRefundAmount = feeDto.getRefundAmount().add(totalRefundAmount);
                }
            }
        }

       return totalRefundAmount;
    }

    public boolean isContainsPaymentsRefundRole (){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean containsPaymentsRefundRole = false;

        Iterator<? extends GrantedAuthority> userRole =  authentication.getAuthorities().iterator();

        while (userRole.hasNext()){

            String nextUser = userRole.next().toString();

            if(nextUser.equals("payments-refund")){
                containsPaymentsRefundRole = true;
                break;
            }

            if(nextUser.equals("payments-refund-approver")){
                containsPaymentsRefundRole = true;
                break;
            }
        }
        return containsPaymentsRefundRole;
    }


       public boolean checkRefundsRole(PaymentGroupDto paymentGroupDto){
        if(null != paymentGroupDto && null != paymentGroupDto.getPayments()) {
            for (PaymentDto payment : paymentGroupDto.getPayments()) {
                LOG.info("Payment getRefundEnable --> {}",payment.getRefundEnable() );
                if (payment.getRefundEnable() != null && payment.getRefundEnable()) {
                    return true;
                }
            }
        }
        return false;
    }

    public BigDecimal getAvailableBalance(PaymentGroupDto paymentGroupDto, RefundListDtoResponse refundListDtoResponse){

        BigDecimal totalPaymentAmount = new BigDecimal(BigInteger.ZERO);
        BigDecimal totalRefundAmount = new BigDecimal(BigInteger.ZERO);

        //Goes through each payment in this paymentDto, and get the total payment amount
        if(null != paymentGroupDto.getPayments()){
            for (PaymentDto payment : paymentGroupDto.getPayments()) {
                LOG.info("INSIDE PAYMENT GROUP LOOP");


                if(payment.getStatus() == "Success") {
                    totalPaymentAmount = totalPaymentAmount.add(payment.getAmount());
                    LOG.info("PAYMENT IS SUCCESS");

                }
                //if theres a refund available for this payment, get the total refund amount
                if (refundListDtoResponse != null) {
                    for (RefundDto refundDto : refundListDtoResponse.getRefundList()) {

                        LOG.info("INSIDE REFUND GROUP LOOP");
                        LOG.info("PAYMENT REF: {}", payment.getPaymentReference());
                        LOG.info("REFUND PAYMENT REF: {}", refundDto.getPaymentReference());

                        //Condition to check that a valid refund corresponding with the payment reference is considered only
                        if (refundDto.getPaymentReference().equalsIgnoreCase(payment.getPaymentReference()) || refundDto.getPaymentReference().equalsIgnoreCase(payment.getReference())){
                            LOG.info("IF REFUND PAYMENT REF MATCHES PAYMENT REF, REFUND STATUS: {}", refundDto.getRefundStatus().getName());

                            if((refundDto.getRefundStatus().getName().equalsIgnoreCase("Accepted") || refundDto.getRefundStatus().getName().equalsIgnoreCase("Approved")
                                || refundDto.getRefundStatus().getName().equalsIgnoreCase("Sent for approval"))) {
                                totalRefundAmount = totalRefundAmount.add(refundDto.getAmount());
                            }
                    }
                }
                }
            }
        }
        LOG.info("TOTAL PAYMENT {}", totalPaymentAmount);
        LOG.info("TOTAL REFUND AMOUNT {}", totalRefundAmount);

        return totalPaymentAmount.subtract(totalRefundAmount);
    }

    public List<List<String>>  getAllRefundedFeeIds(RefundListDtoResponse refundListDtoResponse){
        List<String> refundedFees = new ArrayList<String>();
        List<List<String>> refundsFeeList = new ArrayList<>();

        if(refundListDtoResponse !=null) {
            for (RefundDto refundDto : refundListDtoResponse.getRefundList()) {
                if (refundDto.getReason().equals("Retrospective remission")) {
                  refundedFees =  Arrays.asList(refundDto.getFeeIds().split(","));
                    refundsFeeList.add(refundedFees);
                }

            }
        }
        return refundsFeeList;
    }



    public PaymentGroupDto checkRefundAgainstRemissionFeeApportionV2(MultiValueMap<String, String> headers,
                                                                     PaymentGroupDto paymentGroupDto, String paymentReference) {
        //check roles
        if(isContainsPaymentsRefundRole()) {

            Payment payment = paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
            BigDecimal balanceAvailable;
            Boolean refundRole;
            List<String> paymentList = new ArrayList<>();
            for (PaymentDto payment1 : paymentGroupDto.getPayments()) {

                paymentList.add(payment1.getReference());
            }
            Optional<List<PaymentFailures>> paymentFailuresList = paymentFailureRepository.findFailedPayments(paymentList);

            //get the RefundListDtoResponse by calling refunds app
            RefundListDtoResponse refundListDtoResponse = getRefundsFromRefundService(payment.getCcdCaseNumber(), headers);
            LOG.info("refundListDtoResponse : {}", refundListDtoResponse);

            //gets a list of all the refunded fee ids for this case
            List<List<String>> refundedFees= getAllRefundedFeeIds(refundListDtoResponse);

            for(PaymentDto paymentDto : paymentGroupDto.getPayments()){
                LOG.info("INSIDE MAIN LOOP");

                boolean activeRemission = false;
                refundRole = checkRefundsRole(paymentGroupDto);
                balanceAvailable = getAvailableBalance(paymentGroupDto, refundListDtoResponse);

                //Main check: if the payment has refund role and there is available balance
                if(refundRole && balanceAvailable.compareTo(BigDecimal.ZERO) > 0){
                    LOG.info("BALANCE IS AVAILABLE");

                    //Goes through each fee in a paymentGroupDto
                    for (FeeDto fee : paymentGroupDto.getFees()) {
                        LOG.info("INSIDE FEE LOOP");

                        //Check that there is a remission object
                        if(!paymentGroupDto.getRemissions().isEmpty()){
                            LOG.info("THERE IS A REMISSION");

                            //Goes through each remission in paymentGroupDto
                            for (RemissionDto remission : paymentGroupDto.getRemissions()) {
                                LOG.info("INSIDE REMISSION LOOP");

                                //Makes sure that the fee ID matches with the fee ID in remission
                                if (fee.getId() == remission.getFeeId()) {
                                    LOG.info("FEE ID MATCHES REMISSION FEE ID");


                                    //IF THERE IS NO PROCESSED REFUND FOR THE FEE BUT THERE IS AN ACTIVE REMISSION
                                    if (!refundedFees.stream().flatMap(List::stream).anyMatch(fee.getId().toString()::equals)
                                        && fee.getId() == remission.getFeeId()) {
                                        LOG.info("ENTERED NO PROCESSED REFUND IF");

                                        activeRemission =true;
                                        fee.setAddRemission(false);
                                        remission.setAddRefund(true);
                                        paymentDto.setIssueRefund(false);
                                    }
                                    //IF THERE IS A PROCESSED REFUND FOR THE FEE
                                    else if (refundedFees.stream().flatMap(List::stream).anyMatch(fee.getId().toString()::equals)) {
                                        LOG.info("ENTERED PROCESSED REFUND ELSEIF");

                                        fee.setAddRemission(false);
                                        remission.setAddRefund(false);
                                        paymentDto.setIssueRefund(true);

                                    }
                                    //NO PROCESSED OR OUTSTANDING REMISSION
                                    else if (!refundedFees.stream().flatMap(List::stream).anyMatch(fee.getId().toString()::equals)
                                        && fee.getId() != remission.getFeeId()) {
                                        LOG.info("ENTERED NO PROCESSED OR OUTSTANDING REFUND ELSEIF");

                                        fee.setAddRemission(true);
                                        remission.setAddRefund(false);
                                        paymentDto.setIssueRefund(true);
                                    }
                                }
                                //If the fee does not have a remission, check if theres any other active remissions in this payment group
                                else{
                                    LOG.info("FEE ID DOESNT MATCH REMISSION FEE ID");
                                    if(activeRemission){
                                        fee.setAddRemission(false);
                                    }else {
                                        fee.setAddRemission(true);
                                    }
                                }
                                        if (paymentFailuresList.isPresent()) {

                                            paymentFailuresList.get().forEach(paymentFailures -> {

                                                if (paymentFailures.getPaymentReference().equals(paymentDto.getReference())) {
                                                    remission.setAddRefund(false);

                                                }
                                            });
                                        }
                            }
                        }
                        else{
                            LOG.info("THERE IS NO REMISSION");

                            paymentDto.setIssueRefund(true);
                            fee.setAddRemission(true);
                        }
                    }

                    for (FeeDto fee : paymentGroupDto.getFees()) {
                        if(activeRemission){
                            fee.setAddRemission(false);
                        }
                    }


                    paymentGroupDto.getPayments().forEach(paymentDto1 -> {
                        if(refundListDtoResponse != null) {

                            refundListDtoResponse.getRefundList().forEach(refundDto -> {

                                if (refundDto.getPaymentReference().equals(paymentDto1.getReference())) {
                                    paymentDto1.setOverPayment(BigDecimal.ZERO);
                                }
                            });
                        }else {
                            if(paymentDto.getOverPayment().compareTo(BigDecimal.ZERO)>0){

                                paymentDto.setIssueRefund(true);
                                paymentGroupDto.getFees().forEach(feeDto -> {
                                    feeDto.setAddRemission(false);
                                    feeDto.setRemissionEnable(false);
                                });
                                if(!paymentGroupDto.getRemissions().isEmpty()) {
                                    paymentGroupDto.getRemissions().forEach(remissionDto -> {
                                        remissionDto.setAddRefund(false);

                                    });
                                }

                            }

                        }
                    });

                    paymentGroupDto.getFees().forEach(feeDto -> {

                    if(refundListDtoResponse != null) {
                        refundListDtoResponse.getRefundList().forEach(refundDto -> {

                            if (refundDto.getCcdCaseNumber().equals(feeDto.getCcdCaseNumber()) &&
                                (Arrays.stream(refundDto.getFeeIds().split(",")).anyMatch(feeDto.getId().toString()::equals))
                            ) {
                                feeDto.setOverPayment(BigDecimal.ZERO);
                            }
                        });
                    }
                    });
                }
                        if (paymentFailuresList.isPresent()) {

                            paymentFailuresList.get().forEach(paymentFailures -> {

                                if (paymentFailures.getPaymentReference().equals(paymentDto.getReference())) {
                                    paymentDto.setIssueRefund(false);

                                }
                            });
                        }
                    }
        }
        LOG.info("Before returning paymentGroupDto from checkRefundAgainstRemissionFeeApportion {}", paymentGroupDto.getPaymentGroupReference());
        return paymentGroupDto;
    }


    public PaymentGroupResponse checkRefundAgainstRemissionV2(MultiValueMap<String, String> headers,
                                                              PaymentGroupResponse paymentGroupResponse, String ccdCaseNumber) {
        LOG.info("inside checkRefundAgainstRemissionV2 method");
        //check roles
        if(isContainsPaymentsRefundRole()) {

            BigDecimal balanceAvailable;
            Boolean refundRole;

            List<String> paymentList = new ArrayList<>();
            for (PaymentGroupDto paymentGroupDto : paymentGroupResponse.getPaymentGroups()) {

                if(null != paymentGroupDto.getPayments()) {
                    for (PaymentDto payment : paymentGroupDto.getPayments()) {

                        paymentList.add(payment.getReference());
                    }
                }

            }

            Optional<List<PaymentFailures>> paymentFailuresList = paymentFailureRepository.findFailedPayments(paymentList);

            //get the RefundListDtoResponse by calling refunds app
            RefundListDtoResponse refundListDtoResponse = getRefundsFromRefundService(ccdCaseNumber, headers);
            LOG.info("refundListDtoResponse : {}", refundListDtoResponse);

            //gets a list of all the refunded fee ids for this case
            List<List<String>> refundedFees= getAllRefundedFeeIds(refundListDtoResponse);

            for(PaymentGroupDto paymentGroupDto : paymentGroupResponse.getPaymentGroups()){

                boolean activeRemission = false;
                refundRole = checkRefundsRole(paymentGroupDto);
                balanceAvailable = getAvailableBalance(paymentGroupDto, refundListDtoResponse);

                //Main check: if the payment has refund role and there is available balance
                if(refundRole && balanceAvailable.compareTo(BigDecimal.ZERO) > 0){

                    //Goes through each fee in a paymentGroupDto
                    for (FeeDto fee : paymentGroupDto.getFees()) {
                        //Check that there is a remission object
                        if(!paymentGroupDto.getRemissions().isEmpty()){
                            //Goes through each remission in paymentGroupDto
                            for (RemissionDto remission : paymentGroupDto.getRemissions()) {
                                //Makes sure that the fee ID matches with the fee ID in remission
                                if (fee.getId() == remission.getFeeId()) {

                                    //IF THERE IS NO PROCESSED REFUND FOR THE FEE BUT THERE IS AN ACTIVE REMISSION

                                    if (!refundedFees.stream().flatMap(List::stream).anyMatch(fee.getId().toString()::equals)
                                        && fee.getId() == remission.getFeeId()) {
                                        LOG.info("ENTERED NO PROCESSED REFUND IF");

                                        activeRemission =true;
                                        fee.setAddRemission(false);
                                        remission.setAddRefund(true);
                                        for (PaymentDto payment : paymentGroupDto.getPayments()) {
                                            payment.setIssueRefund(false);
                                        }
                                    }
                                    //IF THERE IS A PROCESSED REFUND FOR THE FEE
                                    else if (refundedFees.stream().flatMap(List::stream).anyMatch(fee.getId().toString()::equals)) {
                                        LOG.info("ENTERED PROCESSED REFUND ELSEIF");

                                        fee.setAddRemission(false);
                                        remission.setAddRefund(false);
                                        for (PaymentDto payment : paymentGroupDto.getPayments()) {
                                            payment.setIssueRefund(true);
                                        }
                                    }
                                    //NO PROCESSED OR OUTSTANDING REMISSION
                                    else if (!refundedFees.stream().flatMap(List::stream).anyMatch(fee.getId().toString()::equals)
                                        && fee.getId() != remission.getFeeId()) {
                                        LOG.info("ENTERED NO PROCESSED OR OUTSTANDING REFUND ELSEIF");

                                        fee.setAddRemission(true);
                                        remission.setAddRefund(false);
                                        for (PaymentDto payment : paymentGroupDto.getPayments()) {
                                            payment.setIssueRefund(true);
                                        }
                                    }
                                    paymentGroupDto.getPayments().forEach(paymentDto -> {
                                            if (paymentFailuresList.isPresent()) {
                                                LOG.info("ENTERED PAYMENT FAILURE FOUND FOR REMISSION");
                                                paymentFailuresList.get().forEach(paymentFailures -> {

                                                    if (paymentFailures.getPaymentReference().equals(paymentDto.getReference())) {
                                                        LOG.info("ENTERED PAYMENT FAILURE FOUND FOR REMISSION1");
                                                        remission.setAddRefund(false);

                                                    }
                                                });
                                            }
                                        }
                                    );
                                }
                                //If the fee does not have a remission, check if theres any other active remissions in this payment group
                                else{
                                    if(activeRemission){
                                        fee.setAddRemission(false);
                                    }else {
                                        fee.setAddRemission(true);
                                    }
                                }
                            }
                        }
                        else{
                            for (PaymentDto payment : paymentGroupDto.getPayments()) {
                                payment.setIssueRefund(true);
                            }
                            fee.setAddRemission(true);
                        }
                    }

                    for (FeeDto fee : paymentGroupDto.getFees()) {
                        if(activeRemission){
                            fee.setAddRemission(false);
                        }
                    }


                    //overpayment

                    paymentGroupDto.getFees().forEach(feeDto -> {
                        if (refundListDtoResponse != null) {
                            refundListDtoResponse.getRefundList().forEach(refundDto -> {

                            if (refundDto.getCcdCaseNumber().equals(feeDto.getCcdCaseNumber())
                            ) {
                                feeDto.setOverPayment(BigDecimal.ZERO);
                            }
                        });
                        }
                    });

                    paymentGroupDto.getPayments().forEach(paymentDto -> {
                        if (refundListDtoResponse != null) {

                            refundListDtoResponse.getRefundList().forEach(refundDto -> {

                                if (refundDto.getPaymentReference().equals(paymentDto.getReference())) {
                                    paymentDto.setOverPayment(BigDecimal.ZERO);
                                }
                            });
                        } else {
                            if(paymentDto.getOverPayment().compareTo(BigDecimal.ZERO)>0){

                                paymentDto.setIssueRefund(true);
                                paymentGroupDto.getFees().forEach(feeDto -> {
                                    feeDto.setAddRemission(false);
                                    feeDto.setRemissionEnable(false);
                                });
                                if(!paymentGroupDto.getRemissions().isEmpty()) {
                                    paymentGroupDto.getRemissions().forEach(remissionDto -> {
                                        remissionDto.setAddRefund(false);

                                    });
                                }

                            }

                        }
                    }
                    );

                    paymentGroupDto.getPayments().forEach(paymentDto -> {
                        if (paymentFailuresList.isPresent()) {
                            LOG.info("ENTERED PAYMENT FAILURE FOUND FOR PAYMENT");
                            paymentFailuresList.get().forEach(paymentFailures -> {

                                if (paymentFailures.getPaymentReference().equals(paymentDto.getReference())) {
                                    LOG.info("ENTERED PAYMENT FAILURE FOUND FOR PAYMENT1");
                                    paymentDto.setIssueRefund(false);

                                }
                            });
                        }
                    }
                    );
                }
            }
        }
        return paymentGroupResponse;
    }

  public void deleteByRefundReference(String refundReference, MultiValueMap<String, String> headers) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refundApiUrl + REFUND_ENDPOINT + "/" + refundReference);
        LOG.debug("builder.toUriString() : {}", builder.toUriString());
        try {
            ResponseEntity<InternalRefundResponse> refundResponseResponseEntity = restTemplateRefundsGroup
                    .exchange(builder.toUriString(), HttpMethod.DELETE, createEntity(headers), InternalRefundResponse.class);
            InternalRefundResponse refundResponse = refundResponseResponseEntity.hasBody() ? refundResponseResponseEntity.getBody() : null;
        } catch (HttpClientErrorException e) {
            LOG.error("client err ", e);
            throw new InvalidRefundRequestException(e.getResponseBodyAsString());
        }
    }
}
