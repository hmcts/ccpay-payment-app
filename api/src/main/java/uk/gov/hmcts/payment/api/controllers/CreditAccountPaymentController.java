package uk.gov.hmcts.payment.api.controllers;


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
import uk.gov.hmcts.payment.api.PaymentFeaturesChecker;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@Api(value = "CreditAccountPaymentController", description = "Credit account payment REST API")
public class CreditAccountPaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(CreditAccountPaymentController.class);

    private final CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;
    private final CreditAccountDtoMapper creditAccountDtoMapper;
    private final AccountService<AccountDto, String> accountService;
    private final PaymentFeaturesChecker paymentFeaturesChecker;


    @Autowired
    public CreditAccountPaymentController(@Qualifier("loggingCreditAccountPaymentService") CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService,
                                          CreditAccountDtoMapper creditAccountDtoMapper,
                                          AccountService<AccountDto, String> accountService,
                                          PaymentFeaturesChecker paymentFeaturesChecker) {
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
        this.accountService = accountService;
        this.paymentFeaturesChecker = paymentFeaturesChecker;
    }

    @ApiOperation(value = "Create credit account payment", notes = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 404, message = "Account information could not be found"),
        @ApiResponse(code = 504, message = "Unable to retrieve account information, please try again later"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/credit-account-payments")
    @ResponseBody
    public ResponseEntity<PaymentDto> createCreditAccountPayment(@Valid @RequestBody CreditAccountPaymentRequest creditAccountPaymentRequest) throws CheckDigitException {
        String paymentGroupReference = PaymentReference.getInstance().getNext();

        Payment payment = Payment.paymentWith()
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
            .build();

        List<PaymentFee> fees = creditAccountPaymentRequest.getFees().stream()
            .map(f -> creditAccountDtoMapper.toFee(f))
            .collect(Collectors.toList());
        LOG.debug("Create credit account request for PaymentGroupRef:" + paymentGroupReference + " ,with Payment and " + fees.size() + " - Fees");

        if (paymentFeaturesChecker.isAccountStatusCheckRequired()) {
            AccountDto accountDetails;
            try {
                accountDetails = accountService.retrieve(creditAccountPaymentRequest.getAccountNumber());
            } catch (HttpClientErrorException ex) {
                LOG.error("Account information could not be found, exception: {}",  ex.getMessage());
                throw new AccountNotFoundException("Account information could not be found");
            } catch (Exception ex) {
                LOG.error("Unable to retrieve account information, exception: {}", ex.getMessage());
                throw new AccountServiceUnavailableException("Unable to retrieve account information, please try again later");
            }

            if (accountDetails.getStatus() == AccountStatus.ACTIVE) {
                payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("success").build());
            } else if (accountDetails.getStatus() == AccountStatus.INACTIVE) {
                payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("failed").build());
            }
        } else {
            payment.setPaymentStatus(PaymentStatus.paymentStatusWith().name("pending").build());
        }

        PaymentFeeLink paymentFeeLink = creditAccountPaymentService.create(payment, fees, paymentGroupReference);

        return new ResponseEntity<>(creditAccountDtoMapper.toCreateCreditAccountPaymentResponse(paymentFeeLink), HttpStatus.CREATED);
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
}
