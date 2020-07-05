package uk.gov.hmcts.payment.api.controllers;


import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.ff4j.FF4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.model.exceptions.DuplicatePaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Api(tags = {"Credit Account Payment"})
public class CreditAccountPaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(CreditAccountPaymentController.class);

    private static final String FAILED = "failed";
    private final static String PAYMENT_CHANNEL_ONLINE = "online";

    private final CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;
    private final CreditAccountDtoMapper creditAccountDtoMapper;
    private final AccountService<AccountDto, String> accountService;
    private final DuplicatePaymentValidator paymentValidator;
    private final FF4j ff4j;
    private final FeePayApportionService feePayApportionService;
    private final LaunchDarklyFeatureToggler featureToggler;
    private final FeePayApportionRepository feePayApportionRepository;
    private final PaymentFeeRepository paymentFeeRepository;


    @Autowired
    public CreditAccountPaymentController(@Qualifier("loggingCreditAccountPaymentService") CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService,
                                          CreditAccountDtoMapper creditAccountDtoMapper,
                                          AccountService<AccountDto, String> accountService,
                                          DuplicatePaymentValidator paymentValidator, FF4j ff4j,
                                          FeePayApportionService feePayApportionService,LaunchDarklyFeatureToggler featureToggler,
                                          FeePayApportionRepository feePayApportionRepository,
                                          PaymentFeeRepository paymentFeeRepository) {
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
        this.accountService = accountService;
        this.paymentValidator = paymentValidator;
        this.ff4j = ff4j;
        this.feePayApportionService = feePayApportionService;
        this.featureToggler = featureToggler;
        this.feePayApportionRepository = feePayApportionRepository;
        this.paymentFeeRepository = paymentFeeRepository;
    }

    @ApiOperation(value = "Create credit account payment", notes = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 403, message = "Payment failed due to insufficient funds or the account being on hold"),
        @ApiResponse(code = 404, message = "Account information could not be found"),
        @ApiResponse(code = 504, message = "Unable to retrieve account information, please try again later"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/credit-account-payments")
    @ResponseBody
    public ResponseEntity<PaymentDto> createCreditAccountPayment(@Valid @RequestBody CreditAccountPaymentRequest creditAccountPaymentRequest) throws CheckDigitException {
        String paymentGroupReference = PaymentReference.getInstance().getNext();

        final Payment payment = createPaymentInstanceFromRequest(creditAccountPaymentRequest);

        List<PaymentFee> fees = creditAccountPaymentRequest.getFees().stream()
            .map(f -> creditAccountDtoMapper.toFee(f))
            .collect(Collectors.toList());

        fees.stream().forEach(fee -> {
            fee.setCcdCaseNumber((fee.getCcdCaseNumber() != null || !fee.getCcdCaseNumber().isEmpty())
                ? fee.getCcdCaseNumber()
                : creditAccountPaymentRequest.getCcdCaseNumber());
        });

        LOG.debug("Create credit account request for PaymentGroupRef:" + paymentGroupReference + " ,with Payment and " + fees.size() + " - Fees");

        LOG.info("CreditAccountPayment received for ccdCaseNumber : {} serviceType : {} pbaNumber : {} amount : {} NoOfFees : {}",
            payment.getCcdCaseNumber(), payment.getServiceType(), payment.getPbaNumber(), payment.getAmount(), fees.size());
        if (isAccountStatusCheckRequired(creditAccountPaymentRequest.getService())) {
            LOG.info("Checking with Liberata for Service : {}", creditAccountPaymentRequest.getService());

            AccountDto accountDetails;
            try {
                accountDetails = accountService.retrieve(creditAccountPaymentRequest.getAccountNumber());
                LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {}", payment.getCcdCaseNumber(), accountDetails.getStatus());
            } catch (HttpClientErrorException ex) {
                LOG.error("Account information could not be found, exception: {}", ex.getMessage());
                throw new AccountNotFoundException("Account information could not be found");
            } catch (Exception ex) {
                LOG.error("Unable to retrieve account information, exception: {}", ex.getMessage());
                throw new AccountServiceUnavailableException("Unable to retrieve account information, please try again later");
            }

            setPaymentStatus(creditAccountPaymentRequest, payment, accountDetails);
        } else {
            LOG.info("Setting status to pending");
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("pending").build());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} PaymentStatus : {} - Account Balance Sufficient!!!", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
        }

        checkDuplication(payment, fees);

        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.create(payment, fees, paymentGroupReference);

        if (payment.getPaymentStatus().getName().equals(FAILED)) {
            LOG.info("CreditAccountPayment Response 403(FORBIDDEN) for ccdCaseNumber : {} PaymentStatus : {}", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
            return new ResponseEntity<>(creditAccountDtoMapper.toCreateCreditAccountPaymentResponse(paymentFeeLink), HttpStatus.FORBIDDEN);
        }

        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        LOG.info("ApportionFeature Flag Value in CreditAccountPaymentController : {}", apportionFeature);
        if(apportionFeature) {
            Payment pbaPayment = paymentFeeLink.getPayments().get(0);
            feePayApportionService.processApportion(pbaPayment);

            // Update Fee Amount Due as Payment Status received from Bulk Scan Payment as SUCCESS
            if(Lists.newArrayList("success", "pending").contains(pbaPayment.getPaymentStatus().getName().toLowerCase())) {
                LOG.info("Update Fee Amount Due as Payment Status received from PBA Payment as {}" + pbaPayment.getPaymentStatus().getName());
                updateFeeAmountDue(payment);
            }
        }

        LOG.info("CreditAccountPayment Response 201(CREATED) for ccdCaseNumber : {} PaymentStatus : {}", payment.getCcdCaseNumber(), payment.getPaymentStatus().getName());
        return new ResponseEntity<>(creditAccountDtoMapper.toCreateCreditAccountPaymentResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    private void updateFeeAmountDue(Payment payment) {
        if(feePayApportionRepository.findByPaymentId(payment.getId()).isPresent()) {
            feePayApportionRepository.findByPaymentId(payment.getId()).get().stream()
                .forEach(feePayApportion -> {
                    PaymentFee fee = paymentFeeRepository.findById(feePayApportion.getFeeId()).get();
                    fee.setAmountDue(fee.getAmountDue().subtract(feePayApportion.getApportionAmount()));
                    paymentFeeRepository.save(fee);
                    LOG.info("Updated FeeId " + fee.getId() + " as PaymentId " + payment.getId() + " Status Changed to " + payment.getPaymentStatus().getName());
                });
        }
    }

    private Payment createPaymentInstanceFromRequest(@RequestBody @Valid CreditAccountPaymentRequest creditAccountPaymentRequest) {
        return Payment.paymentWith()
            .amount(creditAccountPaymentRequest.getAmount())
            .description(creditAccountPaymentRequest.getDescription())
            .ccdCaseNumber(creditAccountPaymentRequest.getCcdCaseNumber())
            .caseReference(creditAccountPaymentRequest.getCaseReference())
            .currency(creditAccountPaymentRequest.getCurrency().getCode())
            .serviceType(creditAccountPaymentRequest.getService().getName())
            .customerReference(creditAccountPaymentRequest.getCustomerReference())
            .organisationName(creditAccountPaymentRequest.getOrganisationName())
            .pbaNumber(creditAccountPaymentRequest.getAccountNumber())
            .siteId(creditAccountPaymentRequest.getSiteId())
            .paymentChannel(PaymentChannel.paymentChannelWith().name(PAYMENT_CHANNEL_ONLINE).build())
            .build();
    }

    private void setPaymentStatus(@RequestBody @Valid CreditAccountPaymentRequest creditAccountPaymentRequest, Payment payment, AccountDto accountDetails) {
        if (accountDetails.getStatus() == AccountStatus.ACTIVE && isAccountBalanceSufficient(accountDetails.getAvailableBalance(),
            creditAccountPaymentRequest.getAmount())) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance Sufficient!!!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.ACTIVE) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0001")
                .message("You have insufficient funds available")
                .message("Payment request failed. PBA account " + accountDetails.getAccountName()
                    + " have insufficient funds available").build()));
            LOG.info("Payment request failed. PBA account {} has insufficient funds available." +
                    " Requested payment was {} where available balance is {}",
                accountDetails.getAccountName(), creditAccountPaymentRequest.getAmount(),
                accountDetails.getAvailableBalance());
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance InSufficient!!!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.ON_HOLD) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0003")
                .message("Your account is on hold")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance InSufficient!!!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        } else if (accountDetails.getStatus() == AccountStatus.DELETED) {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name(FAILED).build());
            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(payment.getPaymentStatus().getName())
                .errorCode("CA-E0004")
                .message("Your account is deleted")
                .build()));
            LOG.info("CreditAccountPayment received for ccdCaseNumber : {} Liberata AccountStatus : {} PaymentStatus : {} - Account Balance InSufficient!!!", payment.getCcdCaseNumber(), accountDetails.getStatus(), payment.getPaymentStatus().getName());
        }
    }

    @ApiOperation(value = "Get credit account payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/credit-account-payments/{paymentReference}", method = GET)
    public ResponseEntity<PaymentDto> retrieve(@PathVariable("paymentReference") String paymentReference) {
        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.retrieveByPaymentReference(paymentReference);
        Payment payment = paymentFeeLink.getPayments().stream().filter(p -> p.getReference().equals(paymentReference))
            .findAny()
            .orElseThrow(PaymentNotFoundException::new);
        List<PaymentFee> fees = paymentFeeLink.getFees();
        return new ResponseEntity<>(creditAccountDtoMapper.toRetrievePaymentResponse(payment, fees), HttpStatus.OK);
    }

    @ApiOperation(value = "Get credit account payment statuses by payment reference", notes = "Get payment statuses for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/credit-account-payments/{paymentReference}/statuses", method = GET)
    public ResponseEntity<PaymentDto> retrievePaymentStatus(@PathVariable("paymentReference") String paymentReference) {
        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.retrieveByPaymentReference(paymentReference);
        Payment payment = paymentFeeLink.getPayments().stream().filter(p -> p.getReference().equals(paymentReference))
            .findAny()
            .orElseThrow(PaymentNotFoundException::new);

        return new ResponseEntity<>(creditAccountDtoMapper.toRetrievePaymentStatusResponse(payment), HttpStatus.OK);
    }

    @ExceptionHandler(value = {PaymentNotFoundException.class})
    public ResponseEntity httpClientErrorException() {
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        LOG.error("Error while processing payment request:", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DuplicatePaymentException.class)
    public String returnDuplicateError(DuplicatePaymentException ex) {
        LOG.error("Duplicate pba payments error:", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public String return404(AccountNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(AccountServiceUnavailableException.class)
    public String return504(AccountServiceUnavailableException ex) {
        return ex.getMessage();
    }

    private boolean isAccountStatusCheckRequired(Service service) {
        final String serviceName = service.toString();
        LOG.info("Service.FINREM.getName(): {}" + " service.toString(): {}" + " Service.FINREM.getName().equalsIgnoreCase(service.toString()): {}" +
                " ff4j.check(\"check-liberata-account-for-all-services\"): {}" + " ff4j.check(\"credit-account-payment-liberata-check\"): {}",
            Service.FINREM.getName(), serviceName, Service.FINREM.getName().equalsIgnoreCase(serviceName),
            ff4j.check("check-liberata-account-for-all-services"), ff4j.check("credit-account-payment-liberata-check")
        );

        if (ff4j.check("check-liberata-account-for-all-services")) {
            return true;
        }

        final boolean svcNameChk = Service.FINREM.getName().equalsIgnoreCase(serviceName) || Service.FPL.toString().equalsIgnoreCase(serviceName);
        return ff4j.check("credit-account-payment-liberata-check") && svcNameChk;
    }

    private boolean isAccountBalanceSufficient(BigDecimal availableBalance, BigDecimal paymentAmount) {
        return availableBalance.compareTo(paymentAmount) >= 0;
    }

    private void checkDuplication(Payment payment, List<PaymentFee> fees) {
        if (ff4j.check("duplicate-payment-check")) {
            paymentValidator.checkDuplication(payment, fees);
        }
    }

}
