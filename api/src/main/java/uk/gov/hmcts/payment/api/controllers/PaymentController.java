package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.eclipse.jetty.util.log.Log;
import org.ff4j.FF4j;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

@RestController
@Api(tags = {"Payment"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentController", description = "Payment REST API")})
public class PaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService<PaymentFeeLink, String> paymentService;
    private final CallbackService callbackService;
    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentDtoMapper paymentDtoMapper;
    private final PaymentValidator validator;
    private final FF4j ff4j;
    private final DateTimeFormatter formatter;
    private final PaymentFeeRepository paymentFeeRepository;


    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired()
    @Qualifier("restTemplateIacSupplementaryInfo")
    private RestTemplate restTemplateIacSupplementaryInfo;


    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;

    @Autowired
    public PaymentController(PaymentService<PaymentFeeLink, String> paymentService,
                             PaymentStatusRepository paymentStatusRepository, CallbackService callbackService,
                             PaymentDtoMapper paymentDtoMapper, PaymentValidator paymentValidator, FF4j ff4j,
                             DateUtil dateUtil, PaymentFeeRepository paymentFeeRepository) {
        this.paymentService = paymentService;
        this.callbackService = callbackService;
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentDtoMapper = paymentDtoMapper;
        this.validator = paymentValidator;
        this.ff4j = ff4j;
        this.formatter = dateUtil.getIsoDateTimeFormatter();
        this.paymentFeeRepository = paymentFeeRepository;
    }

    @ApiOperation(value = "Update case reference by payment reference", notes = "Update case reference by payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{reference}", method = PATCH)
    @Transactional
    public ResponseEntity updateCaseReference(@PathVariable("reference") String reference,
                                              @RequestBody @Validated UpdatePaymentRequest request) {
        Optional<Payment> payment = getPaymentByReference(reference);

        if (payment.isPresent()) {
            if (request.getCaseReference() != null) {
                payment.get().setCaseReference(request.getCaseReference());
            }
            if (request.getCcdCaseNumber() != null) {
                payment.get().setCcdCaseNumber(request.getCcdCaseNumber());
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @ApiOperation(value = "GET Supplementary Details", notes = "Get the supplementary details for the associate ccd case numbers")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Supplementary details completely retrieved"),
        @ApiResponse(code = 206, message = "Supplementary details partially retrieved"),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 403, message = "Access denied or unauthorised"),
        @ApiResponse(code = 404, message = "Supplementary details not found for all the case numbers given"),
        @ApiResponse(code = 500, message = "Unexpected or Run time exception")

    })
    @PostMapping(value = "/supplementary-details")
    public ResponseEntity getIacSupplementaryDetails(
        @RequestBody IacSupplementaryRequest iacSupplementaryRequest) throws IOException {

        SupplementaryMainDto supplementaryMainDto = new SupplementaryMainDto();
        List<SupplementaryInfoDto> responselistAdded = new ArrayList<>();

        for(int i=0;i<iacSupplementaryRequest.getCcdCaseNumbers().size();i++){
            SupplementaryInfoDto supplementaryInfoDto= new SupplementaryInfoDto();
            SupplementaryDetailsDto supplementaryDetailsDto = new SupplementaryDetailsDto();
            supplementaryInfoDto.setCcdCaseNumber(iacSupplementaryRequest.getCcdCaseNumbers().get(i));
            supplementaryDetailsDto.setSurname("Alex - " +i );
            supplementaryInfoDto.setSupplementaryDetails(supplementaryDetailsDto);
            responselistAdded.add(supplementaryInfoDto);
        }
        supplementaryMainDto.setSupplementaryInfo(responselistAdded);

        //missing_supplementary_info
        List<String> listMissingSuppInfo = new ArrayList<>();
        listMissingSuppInfo.add("1234123412341234");
        listMissingSuppInfo.add("4321432143214321");
        MissingSupplementaryDetailsDto missingSupplementaryDetailsDto1 = new MissingSupplementaryDetailsDto();
        missingSupplementaryDetailsDto1.setCcdCaseNumbers(listMissingSuppInfo);
        supplementaryMainDto.setMissingSupplementaryInfo(missingSupplementaryDetailsDto1);

        return new ResponseEntity(supplementaryMainDto, HttpStatus.OK);
        //return new ResponseEntity(supplementaryMainDto, HttpStatus.PARTIAL_CONTENT);


    }

    @ApiOperation(value = "Get payments for between dates", notes = "Get list of payments. You can optionally provide start date and end dates which can include times as well. Following are the supported date/time formats. These are yyyy-MM-dd, dd-MM-yyyy," +
        "yyyy-MM-dd HH:mm:ss, dd-MM-yyyy HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss, dd-MM-yyyy'T'HH:mm:ss")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(value = "/payments")
    @PaymentExternalAPI
    public PaymentsResponse retrievePayments(@RequestParam(name = "start_date", required = false) Optional<String> startDateTimeString,
                                             @RequestParam(name = "end_date", required = false) Optional<String> endDateTimeString,
                                             @RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType,
                                             @RequestParam(name = "service_name", required = false) Optional<String> serviceType,
                                             @RequestParam(name = "ccd_case_number", required = false) String ccdCaseNumber,
                                             @RequestParam(name = "pba_number", required = false) String pbaNumber
    ) {

        validatePullRequest(startDateTimeString, endDateTimeString, paymentMethodType, serviceType);

        Date fromDateTime = getFromDateTime(startDateTimeString);

        Date toDateTime = getToDateTime(endDateTimeString, fromDateTime);

        List<PaymentFeeLink> paymentFeeLinks = paymentService
            .search(
                getSearchCriteria(paymentMethodType, serviceType, ccdCaseNumber, pbaNumber, fromDateTime, toDateTime)
            );

        final List<PaymentDto> paymentDtos = new ArrayList<>();
        LOG.info("No of paymentFeeLinks retrieved for Liberata Pull : {}", paymentFeeLinks.size());
        for (final PaymentFeeLink paymentFeeLink: paymentFeeLinks) {
            populatePaymentDtos(paymentDtos, paymentFeeLink, fromDateTime, toDateTime);
        }
        return new PaymentsResponse(paymentDtos);
    }

    @ApiOperation(value = "Get payments for Reconciliation for between dates", notes = "Get list of payments. You can optionally provide start date and end dates which can include times as well. Following are the supported date/time formats. These are yyyy-MM-dd, dd-MM-yyyy," +
        "yyyy-MM-dd HH:mm:ss, dd-MM-yyyy HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss, dd-MM-yyyy'T'HH:mm:ss")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 206, message = "Supplementary details partially retrieved"),
    })
    @GetMapping(value = "/reconciliation-payments")
    @PaymentExternalAPI
    public ResponseEntity<PaymentsResponse> retrievePaymentsWithApportion(@RequestParam(name = "start_date", required = false) Optional<String> startDateTimeString,
                                                        @RequestParam(name = "end_date", required = false) Optional<String> endDateTimeString,
                                                        @RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType,
                                                        @RequestParam(name = "service_name", required = false) Optional<String> serviceType,
                                                        @RequestParam(name = "ccd_case_number", required = false) String ccdCaseNumber,
                                                        @RequestParam(name = "pba_number", required = false) String pbaNumber
    ) {

        validatePullRequest(startDateTimeString, endDateTimeString, paymentMethodType, serviceType);

        Date fromDateTime = getFromDateTime(startDateTimeString);

        Date toDateTime = getToDateTime(endDateTimeString, fromDateTime);

        List<Payment> payments = paymentService
            .searchByCriteria(
                getSearchCriteria(paymentMethodType, serviceType, ccdCaseNumber, pbaNumber, fromDateTime, toDateTime)
            );

        final List<PaymentDto> paymentDtos = new ArrayList<>();
        LOG.info("No of paymentFeeLinks retrieved for Liberata Pull : {}", payments.size());
        populatePaymentDtos(paymentDtos, payments);

        boolean iacSupplementaryDetailsFeature = featureToggler.getBooleanValue("iac-supplementary-details-feature",false);
        LOG.info("IAC Supplementary Details feature flag in liberata API: {}", iacSupplementaryDetailsFeature);
        HttpStatus paymentResponseHttpStatus = HttpStatus.OK;

        if(iacSupplementaryDetailsFeature) {

            //Map of IAC Payments
            Map<String, PaymentDto> iacPaymentDtosMap = new HashMap<>();

            paymentDtos.stream()
                .filter(paymentIac -> (paymentIac.getServiceName().equalsIgnoreCase(Service.IAC.getName())))
                .forEach(paymentDto ->  iacPaymentDtosMap.put(paymentDto.getCcdCaseNumber(), paymentDto));

            if(iacPaymentDtosMap != null && !iacPaymentDtosMap.isEmpty()) {
                LOG.info("No of Iac payments retrieved  : {}", iacPaymentDtosMap.size());
                List<String> iacCcdCaseNos = new ArrayList<>(iacPaymentDtosMap.keySet());
                LOG.info("No of Iac Ccd case numbers  : {}", iacCcdCaseNos.size());

                if (iacCcdCaseNos != null && !iacCcdCaseNos.isEmpty()) {
                    LOG.info("List of IAC Ccd Case numbers : {}", iacCcdCaseNos.toString());
                    ResponseEntity responseEntitySupplementaryInfo = getIacSupplementaryInfo(iacCcdCaseNos);
                    paymentResponseHttpStatus = responseEntitySupplementaryInfo.getStatusCode();
                    populateSupplementaryInfoToPaymentDtos(iacPaymentDtosMap, responseEntitySupplementaryInfo);
                }
            }else{
                LOG.info("No Iac payments retrieved");
            }
        }
                PaymentsResponse paymentsResponse = new PaymentsResponse(paymentDtos);
                return new ResponseEntity(paymentsResponse,paymentResponseHttpStatus);
    }

    private void populateSupplementaryInfoToPaymentDtos(Map<String, PaymentDto> iacPaymentMap, ResponseEntity responseEntitySupplementaryInfo) {

        LOG.info("Response received from IAC supplementary Info Endpoint : {}",responseEntitySupplementaryInfo.getStatusCode() );

        if(responseEntitySupplementaryInfo.getStatusCodeValue() == HttpStatus.OK.value() || responseEntitySupplementaryInfo.getStatusCodeValue() == HttpStatus.PARTIAL_CONTENT.value()) {

            ObjectMapper objectMapperSupplementaryInfo = new ObjectMapper();
            SupplementaryMainDto supplementaryMainDto = objectMapperSupplementaryInfo.convertValue(responseEntitySupplementaryInfo.getBody(), SupplementaryMainDto.class);
            List<SupplementaryInfoDto> lstSupplementaryInfoDto = supplementaryMainDto.getSupplementaryInfo();
            MissingSupplementaryDetailsDto lstMissingSupplementaryInfoDto = supplementaryMainDto.getMissingSupplementaryInfo();

            if(responseEntitySupplementaryInfo.getStatusCodeValue() == HttpStatus.PARTIAL_CONTENT.value() && lstMissingSupplementaryInfoDto == null)
                LOG.info("No missing supplementary info received from IAC for any CCD case numbers, however response is 206");

            if(lstMissingSupplementaryInfoDto != null && lstMissingSupplementaryInfoDto.getCcdCaseNumbers() != null)
                LOG.info("missing supplementary info from IAC for CCD case numbers : {}", lstMissingSupplementaryInfoDto.getCcdCaseNumbers().toString());

            if(lstSupplementaryInfoDto != null && !lstSupplementaryInfoDto.isEmpty()) {
                lstSupplementaryInfoDto.stream().
                    forEach(supplementaryInfoDto -> {
                        PaymentDto iacPaymentDTO = iacPaymentMap.get(supplementaryInfoDto.getCcdCaseNumber());
                        iacPaymentDTO.setSupplementaryInfo(Arrays.asList(supplementaryInfoDto));
                    });
            }else{
                LOG.info("No supplementary info received from IAC Endpoint for any of CCD Case No");
            }
        }
  }

    public ResponseEntity getIacSupplementaryInfo(List<String> iacCcdCaseNos) throws RestClientException {

        IacSupplementaryRequest iacSupplementaryRequest = IacSupplementaryRequest.createIacSupplementaryRequestWith()
            .ccdCaseNumbers(iacCcdCaseNos).build();

        MultiValueMap<String, String> headerMultiValueMapForIacSuppInfo = new LinkedMultiValueMap<String, String>();
        //Below code for auth token will not required as iac need only S2S and not the Auth token : need to remove it later
            List<String> authTokenPaymentList = new ArrayList<>();
            authTokenPaymentList.add("krishnakn00@gmail.com");
            headerMultiValueMapForIacSuppInfo.put("Authorization", authTokenPaymentList);


            List<String> serviceAuthTokenPaymentList = new ArrayList<>();
            //Below code need to be added , it is commented as its not working on local so hardcoded for test
            //Generate token for payment api and replace
            //serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

            //Hard coded need to remove it
                serviceAuthTokenPaymentList.add("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjbWMiLCJleHAiOjE1MzMyMzc3NjN9.3iwg2cCa1_G9-TAMupqsQsIVBMWg9ORGir5xZyPhDabk09Ldk0-oQgDQq735TjDQzPI8AxL1PgjtOPDKeKyxfg[akiss@reformMgmtDevBastion02");

                headerMultiValueMapForIacSuppInfo.put("ServiceAuthorization", serviceAuthTokenPaymentList);

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForIacSuppInfo);
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity<>(iacSupplementaryRequest, headers);
        ResponseEntity  responseEntity = restTemplateIacSupplementaryInfo.exchange(iacSupplementaryInfoUrl+"/supplementary-details",HttpMethod.POST,entity,SupplementaryMainDto.class);

        return responseEntity;
    }


    @ApiOperation(value = "Update payment status by payment reference", notes = "Update payment status by payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No content"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @PaymentExternalAPI
    @PatchMapping("/payments/{reference}/status/{status}")
    @Transactional
    public ResponseEntity updatePaymentStatus(@PathVariable(value = "reference", required = true) String reference,
                                              @PathVariable(value = "status", required = true) String status) {
        Optional<Payment> payment = getPaymentByReference(reference);

        if (payment.isPresent()) {
            payment.get().setPaymentStatus(paymentStatusRepository.findByNameOrThrow(status));
            if (payment.get().getServiceCallbackUrl() != null) {
                callbackService.callback(payment.get().getPaymentLink(), payment.get());
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }



    @ApiOperation(value = "Get payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/payments/{reference}")
    public PaymentDto retrievePayment(@PathVariable("reference") String paymentReference) {

        PaymentFeeLink paymentFeeLink = paymentService.retrieve(paymentReference);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentReference)).findAny();
        Payment payment1 = null;
        if (payment.isPresent()) {
            payment1 = payment.get();
        }
        return paymentDtoMapper.toGetPaymentResponseDtos(payment1);
    }

    private PaymentSearchCriteria getSearchCriteria(@RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType, @RequestParam(name = "service_name", required = false) Optional<String> serviceType, @RequestParam(name = "ccd_case_number", required = false) String ccdCaseNumber, @RequestParam(name = "pba_number", required = false) String pbaNumber, Date fromDateTime, Date toDateTime) {
        return PaymentSearchCriteria
            .searchCriteriaWith()
            .startDate(fromDateTime)
            .endDate(toDateTime)
            .ccdCaseNumber(ccdCaseNumber)
            .pbaNumber(pbaNumber)
            .paymentMethod(paymentMethodType.map(value -> PaymentMethodType.valueOf(value.toUpperCase()).getType()).orElse(null))
            .serviceType(serviceType.map(value -> Service.valueOf(value.toUpperCase()).getName()).orElse(null))
            .build();
    }

    private Date getFromDateTime(@RequestParam(name = "start_date", required = false) Optional<String> startDateTimeString) {
        return Optional.ofNullable(startDateTimeString.map(formatter::parseLocalDateTime).orElse(null))
            .map(LocalDateTime::toDate)
            .orElse(null);
    }

    private void validatePullRequest(@RequestParam(name = "start_date", required = false) Optional<String> startDateTimeString, @RequestParam(name = "end_date", required = false) Optional<String> endDateTimeString, @RequestParam(name = "payment_method", required = false) Optional<String> paymentMethodType, @RequestParam(name = "service_name", required = false) Optional<String> serviceType) {
        if (!ff4j.check("payment-search")) {
            throw new PaymentException("Payment search feature is not available for usage.");
        }

        validator.validate(paymentMethodType, serviceType, startDateTimeString, endDateTimeString);
    }

    private Date getToDateTime(@RequestParam(name = "end_date", required = false) Optional<String> endDateTimeString, Date fromDateTime) {
        return Optional.ofNullable(endDateTimeString.map(formatter::parseLocalDateTime).orElse(null))
            .map(s -> fromDateTime != null && s.getHourOfDay() == 0 ? s.plusDays(1).minusSeconds(1).toDate() : s.toDate())
            .orElse(null);
    }

    private Optional<Payment> getPaymentByReference(String reference) {
        PaymentFeeLink paymentFeeLink = paymentService.retrieve(reference);
        return paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(reference)).findAny();
    }

    private void populatePaymentDtos(final List<PaymentDto> paymentDtos, final PaymentFeeLink paymentFeeLink, Date fromDateTime, Date toDateTime) {
        //Adding this filter to exclude Exela payments if the bulk scan toggle feature is disabled.
        List<Payment> payments = getFilteredListBasedOnBulkScanToggleFeature(paymentFeeLink);

        //Additional filter to retrieve payments within specified Date Range for Liberata Pull
        if (null != fromDateTime && null != toDateTime) {
            payments = payments.stream().filter(payment -> (payment.getDateUpdated().compareTo(fromDateTime) >= 0
                && payment.getDateUpdated().compareTo(toDateTime) <= 0 ))
                .collect(Collectors.toList());
        } else if (null != fromDateTime) {
            payments = payments.stream().filter(payment -> (payment.getDateUpdated().compareTo(fromDateTime) >= 0))
                .collect(Collectors.toList());
        } else if (null != toDateTime) {
            payments = payments.stream().filter(payment -> (payment.getDateUpdated().compareTo(toDateTime) <= 0 ))
                .collect(Collectors.toList());
        }

        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);

        LOG.info("BSP Feature ON : No of Payments retrieved for Liberata Pull : {}", payments.size());
        LOG.info("Apportion feature flag in liberata API: {}", apportionFeature);
        for (final Payment payment: payments) {
            final String paymentReference = paymentFeeLink.getPaymentReference();
            //Apportion logic added for pulling allocation amount
            populateApportionedFees(paymentDtos, paymentFeeLink, apportionFeature, payment, paymentReference);
        }
    }

    private void populatePaymentDtos(final List<PaymentDto> paymentDtos, final List<Payment> payments) {
        //Adding this filter to exclude Exela payments if the bulk scan toggle feature is disabled.
        List<Payment> filteredPayments = getFilteredListBasedOnBulkScanToggleFeature(payments);
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);

        LOG.info("BSP Feature ON : No of Payments retrieved for Liberata Pull : {}", payments.size());
        LOG.info("Apportion feature flag in liberata API: {}", apportionFeature);
        for (final Payment payment: filteredPayments) {
            final String paymentReference = payment.getPaymentLink() != null ? payment.getPaymentLink().getPaymentReference() : null;
            //Apportion logic added for pulling allocation amount
            populateApportionedFees(paymentDtos, payment.getPaymentLink(), apportionFeature, payment, paymentReference);
        }
    }

    private void populateApportionedFees(List<PaymentDto> paymentDtos, PaymentFeeLink paymentFeeLink, boolean apportionFeature, Payment payment, String paymentReference) {
        boolean apportionCheck = payment.getPaymentChannel() != null
            && !payment.getPaymentChannel().getName().equalsIgnoreCase(Service.DIGITAL_BAR.getName());
        LOG.info("Apportion check value in liberata API: {}", apportionCheck);
        List<PaymentFee> fees = paymentFeeLink.getFees();
        boolean isPaymentAfterApportionment = false;
        if (apportionCheck && apportionFeature) {
            LOG.info("Apportion check and feature passed");
            final List<FeePayApportion> feePayApportionList = paymentService.findByPaymentId(payment.getId());
            if(feePayApportionList != null && !feePayApportionList.isEmpty()) {
                LOG.info("Apportion details available in PaymentController");
                fees = new ArrayList<>();
                getApportionedDetails(fees, feePayApportionList);
                isPaymentAfterApportionment = true;
            }
        }
        //End of Apportion logic
        final PaymentDto paymentDto = paymentDtoMapper.toReconciliationResponseDtoForLibereta(payment, paymentReference, fees,ff4j,isPaymentAfterApportionment);
        paymentDtos.add(paymentDto);
    }

    private void getApportionedDetails(List<PaymentFee> fees, List<FeePayApportion> feePayApportionList) {
        LOG.info("Getting Apportionment Details!!!");
        for (FeePayApportion feePayApportion : feePayApportionList)
        {
            Optional<PaymentFee> apportionedFee = paymentFeeRepository.findById(feePayApportion.getFeeId());
            if(apportionedFee.isPresent())
            {
                LOG.info("Apportioned fee is present");
                PaymentFee fee = apportionedFee.get();
                if(feePayApportion.getApportionAmount() != null) {
                    LOG.info("Apportioned Amount is available!!!");
                    BigDecimal allocatedAmount = feePayApportion.getApportionAmount()
                        .add(feePayApportion.getCallSurplusAmount() != null
                            ? feePayApportion.getCallSurplusAmount()
                            : BigDecimal.valueOf(0));
                    LOG.info("Allocated amount in PaymentController: {}", allocatedAmount);
                    fee.setAllocatedAmount(allocatedAmount);
                    fee.setDateApportioned(feePayApportion.getDateCreated());
                }
                fees.add(fee);
            }
        }
    }

    private List<Payment> getFilteredListBasedOnBulkScanToggleFeature(PaymentFeeLink paymentFeeLink) {
        List<Payment> payments = paymentFeeLink.getPayments();
        payments = getPayments(payments);
        return payments;
    }

    private List<Payment> getFilteredListBasedOnBulkScanToggleFeature(List<Payment> payments) {
        payments = getPayments(payments);
        return payments;
    }

    private List<Payment> getPayments(List<Payment> payments) {
        boolean bulkScanCheck = ff4j.check("bulk-scan-check");
        LOG.info("bulkScanCheck value: {}", bulkScanCheck);
        if (!bulkScanCheck) {
            LOG.info("BSP Feature OFF : No of Payments retrieved for Liberata Pull : {}", payments.size());
            payments = Optional.ofNullable(payments)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(payment -> Objects.nonNull(payment.getPaymentChannel()))
                .filter(payment -> Objects.nonNull(payment.getPaymentChannel().getName()))
                .filter(payment -> !payment.getPaymentChannel().getName().equalsIgnoreCase("bulk scan"))
                .collect(Collectors.toList());
        }
        return payments;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}
