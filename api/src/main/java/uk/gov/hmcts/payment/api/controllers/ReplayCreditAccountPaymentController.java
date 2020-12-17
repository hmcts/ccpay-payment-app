package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.ReplayCreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.mapper.CreditAccountPaymentRequestMapper;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;

import javax.validation.Valid;
import java.util.List;

@RestController
@Api(tags = {"Replay Credit Account Payment"})
public class ReplayCreditAccountPaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayCreditAccountPaymentController.class);

    private static final String FAILED = "failed";


    private final CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;
    private final CreditAccountDtoMapper creditAccountDtoMapper;
    private final AccountService<AccountDto, String> accountService;
    private final DuplicatePaymentValidator paymentValidator;
    private final FeePayApportionService feePayApportionService;
    private final LaunchDarklyFeatureToggler featureToggler;
    private final PBAStatusErrorMapper pbaStatusErrorMapper;
    private final CreditAccountPaymentRequestMapper requestMapper;
    private final List<String> pbaConfig1ServiceNames;


    @Autowired
    public ReplayCreditAccountPaymentController(@Qualifier("loggingCreditAccountPaymentService") CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService,
                                                CreditAccountDtoMapper creditAccountDtoMapper,
                                                AccountService<AccountDto, String> accountService,
                                                DuplicatePaymentValidator paymentValidator,
                                                FeePayApportionService feePayApportionService, LaunchDarklyFeatureToggler featureToggler,
                                                PBAStatusErrorMapper pbaStatusErrorMapper,
                                                CreditAccountPaymentRequestMapper requestMapper, @Value("#{'${pba.config1.service.names}'.split(',')}") List<String> pbaConfig1ServiceNames) {
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
        this.accountService = accountService;
        this.paymentValidator = paymentValidator;
        this.feePayApportionService = feePayApportionService;
        this.featureToggler = featureToggler;
        this.pbaStatusErrorMapper = pbaStatusErrorMapper;
        this.requestMapper = requestMapper;
        this.pbaConfig1ServiceNames = pbaConfig1ServiceNames;
    }

    @ApiOperation(value = "Replay credit account payment", notes = "Replay credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Replay Payment Completed Successfully"),
        @ApiResponse(code = 400, message = "BAD Request"),
        @ApiResponse(code = 500, message = "Replay Payment failed")
    })
    @PostMapping(value = "/replay-credit-account-payments")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> createCreditAccountPayment(@Valid @RequestBody ReplayCreditAccountPaymentRequest replayCreditAccountPaymentRequest) throws CheckDigitException {

        return new ResponseEntity<String>("", HttpStatus.OK);
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
}

