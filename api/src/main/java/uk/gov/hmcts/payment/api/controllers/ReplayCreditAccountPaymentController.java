package uk.gov.hmcts.payment.api.controllers;


import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.mapper.CreditAccountPaymentRequestMapper;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.ReplayCreditAccountPaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    private final ReplayCreditAccountPaymentService replayCreditAccountPaymentService;
    private final CreditAccountPaymentController creditAccountPaymentController;

    private final static String PAYMENT_STATUS_HISTORY_MESSAGE = "System Failure. Not charged";


    @Autowired
    public ReplayCreditAccountPaymentController(@Qualifier("loggingCreditAccountPaymentService") CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService,
                                                CreditAccountDtoMapper creditAccountDtoMapper,
                                                AccountService<AccountDto, String> accountService,
                                                DuplicatePaymentValidator paymentValidator,
                                                FeePayApportionService feePayApportionService, LaunchDarklyFeatureToggler featureToggler,
                                                PBAStatusErrorMapper pbaStatusErrorMapper,
                                                CreditAccountPaymentRequestMapper requestMapper, @Value("#{'${pba.config1.service.names}'.split(',')}") List<String> pbaConfig1ServiceNames,
                                                ReplayCreditAccountPaymentService replayCreditAccountPaymentService,
                                                CreditAccountPaymentController creditAccountPaymentController) {
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
        this.accountService = accountService;
        this.paymentValidator = paymentValidator;
        this.feePayApportionService = feePayApportionService;
        this.featureToggler = featureToggler;
        this.pbaStatusErrorMapper = pbaStatusErrorMapper;
        this.requestMapper = requestMapper;
        this.pbaConfig1ServiceNames = pbaConfig1ServiceNames;
        this.replayCreditAccountPaymentService = replayCreditAccountPaymentService;
        this.creditAccountPaymentController = creditAccountPaymentController;
    }

    @ApiOperation(value = "Replay credit account payment", notes = "Replay credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Replay Payment Completed Successfully"),
        @ApiResponse(code = 400, message = "BAD Request"),
        @ApiResponse(code = 500, message = "Replay Payment failed")
    })
    @PostMapping(value = "/replay-credit-account-payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @Transactional
    public ResponseEntity<String> replayCreditAccountPayment(@RequestParam("csvFile") MultipartFile replayPBAPaymentsFile,
                                                             @RequestParam("isReplayPBAPayments") Boolean isReplayPBAPayments) {

        LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT: isReplayPBAPayments = " + isReplayPBAPayments);

        //Validate csv file
        if (replayPBAPaymentsFile.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            // Parse CSV file to get list of PBA payments for replay
            try (Reader reader = new BufferedReader(new InputStreamReader(replayPBAPaymentsFile.getInputStream()))) {

                // Create csv reader to get list of credit account payments
                CsvToBean<ReplayCreditAccountPaymentRequest> csvToBean = new CsvToBeanBuilder(reader)
                    .withType(ReplayCreditAccountPaymentRequest.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

                // List of replay credit account payment requests
                List<ReplayCreditAccountPaymentRequest> replayCreditAccountPaymentRequestList = csvToBean.parse();
                LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT: CSV TO OBJECT PARSED COUNT = " + replayCreditAccountPaymentRequestList.size());

                List<ReplayCreditAccountPaymentDTO> replayCreditAccountPaymentDTOList = replayCreditAccountPaymentRequestList.stream()
                    .map(replayCreditAccountPaymentRequest -> populateRequestToDTO(isReplayPBAPayments, replayCreditAccountPaymentRequest))
                    .collect(Collectors.toList());

                if (null != replayCreditAccountPaymentDTOList && !replayCreditAccountPaymentDTOList.isEmpty()) {
                    LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT: REQUEST TO DTO COUNT = " + replayCreditAccountPaymentDTOList.size());
                    //a.Update the Payment Status to be 'failed'
                    //b.Update Payment History to reflect Error status and comments 'System Failure. Not charged'
                    replayCreditAccountPaymentDTOList.stream().forEach(replayCreditAccountPaymentDTO -> {
                        try {
                            replayCreditAccountPaymentService.updatePaymentStatusByReference(
                                replayCreditAccountPaymentDTO.getExistingPaymentReference(),
                                PaymentStatus.FAILED, PAYMENT_STATUS_HISTORY_MESSAGE);
                        } catch (Exception exception) {
                            LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT: PBA Payment not found for reference =" + replayCreditAccountPaymentDTO.getExistingPaymentReference());
                        }

                        if (isReplayPBAPayments) {
                            createPBAPayments(replayCreditAccountPaymentDTO);
                        }
                    });
                    LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT:  PROCESS COMPLETED For " + replayCreditAccountPaymentDTOList.size() + " Payments");
                }

            } catch (Exception ex) {
                LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT: Replay PBA Payment failed for All");
                throw new PaymentException(ex);
            }
        }

        return new ResponseEntity<String>("Replay Payment Completed Successfully", HttpStatus.OK);
    }

    private void createPBAPayments(ReplayCreditAccountPaymentDTO replayCreditAccountPaymentDTO) {

        try {
            // d.Call the Payment PBA API v1
            ResponseEntity<PaymentDto> paymentDtoResponseEntity = creditAccountPaymentController.createCreditAccountPayment(replayCreditAccountPaymentDTO.getCreditAccountPaymentRequest());
            if (paymentDtoResponseEntity != null && paymentDtoResponseEntity.getBody() != null) {
                LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT:  Existing Payment Reference : " + replayCreditAccountPaymentDTO.getExistingPaymentReference()
                    + " New Payment Reference : " + paymentDtoResponseEntity.getBody().getReference()
                    + " CCD_CASE_NUMBER : " + replayCreditAccountPaymentDTO.getCreditAccountPaymentRequest().getCcdCaseNumber());
            }

        } catch (Exception exception) {
            LOG.info("REPLAY_CREDIT_ACCOUNT_PAYMENT: Replay PBA Payment ERROR for reference =" + replayCreditAccountPaymentDTO.getExistingPaymentReference());
        }
    }


    private ReplayCreditAccountPaymentDTO populateRequestToDTO(Boolean isReplayPBAPayments,
                                                               ReplayCreditAccountPaymentRequest replayCreditAccountPaymentRequest) {
        if (isReplayPBAPayments) {
            return ReplayCreditAccountPaymentDTO.replayCreditAccountPaymentDTOWith()
                .existingPaymentReference(replayCreditAccountPaymentRequest.getExistingPaymentReference())
                .creditAccountPaymentRequest(CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
                    .amount(replayCreditAccountPaymentRequest.getAmount())
                    .ccdCaseNumber(replayCreditAccountPaymentRequest.getCcdCaseNumber())
                    .accountNumber(replayCreditAccountPaymentRequest.getPbaNumber().replace("\"", ""))
                    .description(replayCreditAccountPaymentRequest.getDescription())
                    .caseReference(replayCreditAccountPaymentRequest.getCaseReference().replace("\"", ""))
                    .service(Service.valueOf(replayCreditAccountPaymentRequest.getService()))
                    .currency(CurrencyCode.valueOf(replayCreditAccountPaymentRequest.getCurrency()))
                    .customerReference(replayCreditAccountPaymentRequest.getCustomerReference())
                    .organisationName(replayCreditAccountPaymentRequest.getOrganisationName().replace("\"", ""))
                    .siteId(replayCreditAccountPaymentRequest.getSiteId())
                    .fees(Collections.singletonList(FeeDto.feeDtoWith()
                        .code(replayCreditAccountPaymentRequest.getCode())
                        .calculatedAmount(replayCreditAccountPaymentRequest.getCalculatedAmount())
                        .version(replayCreditAccountPaymentRequest.getVersion())
                        .build()))
                    .build())
                .build();
        } else {
            return ReplayCreditAccountPaymentDTO.replayCreditAccountPaymentDTOWith()
                .existingPaymentReference(replayCreditAccountPaymentRequest.getExistingPaymentReference())
                .build();
        }
    }

    @ExceptionHandler(value = {PaymentNotFoundException.class})
    public ResponseEntity httpClientErrorException() {
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {PaymentException.class})
    public ResponseEntity returnInternalError(PaymentException ex) {
        return new ResponseEntity(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

