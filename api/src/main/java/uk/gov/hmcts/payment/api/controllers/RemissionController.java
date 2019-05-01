package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.mapper.RemissionDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.RemissionService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.validators.RemissionValidator;

import javax.validation.Valid;

@RestController
@Api(tags = {"Remission"})
@SwaggerDefinition(tags = {@Tag(name = "RemissionController", description = "Remission REST API")})
public class RemissionController {
    private static final Logger LOG = LoggerFactory.getLogger(RemissionController.class);

    @Autowired
    private RemissionService remissionService;

    @Autowired
    private RemissionValidator remissionValidator;

    @Autowired
    private RemissionDtoMapper remissionDtoMapper;

    @ApiOperation(value = "Create full remission record", notes = "Create full remission record")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed"),
    })
    @PostMapping(value = "/remission")
    @ResponseBody
    public ResponseEntity<RemissionDto> createRemission(@Valid @RequestBody RemissionRequest remissionRequest)
        throws CheckDigitException {
        remissionValidator.validate(remissionRequest);

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest);
        PaymentFeeLink paymentFeeLink = remissionService.createRemission(remissionServiceRequest);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Create partial remission record", notes = "Create partial remission record")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed"),
        @ApiResponse(code = 404, message = "Given payment group reference not found")
    })
    @PostMapping(value = "/remission/payment-group/{payment-group-reference}")
    @ResponseBody
    public ResponseEntity<RemissionDto> createPartialRemission(
        @PathVariable("payment-group-reference") String paymentGroupReference,
        @Valid @RequestBody RemissionRequest remissionRequest) throws CheckDigitException {
        remissionValidator.validate(remissionRequest);

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest);
        PaymentFeeLink paymentFeeLink = remissionService.createPartialRemission(remissionServiceRequest, paymentGroupReference);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    private RemissionServiceRequest populateRemissionServiceRequest(RemissionRequest remissionRequest) {
        String paymentGroupReference = PaymentReference.getInstance().getNext();

        return RemissionServiceRequest.remissionServiceRequestWith()
            .paymentGroupReference(paymentGroupReference)
            .hwfAmount(remissionRequest.getHwfAmount())
            .hwfReference(remissionRequest.getHwfReference())
            .beneficiaryName(remissionRequest.getBeneficiaryName())
            .ccdCaseNumber(remissionRequest.getCcdCaseNumber())
            .caseReference(remissionRequest.getCaseReference())
            .siteId(remissionRequest.getSiteId())
            .fee(remissionDtoMapper.toFee(remissionRequest.getFee()))
            .build();

    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String return400onDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOG.error("Error while creating remission", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String return400onDataIntegrityViolation(MethodArgumentNotValidException ex) {
        LOG.error("Error while creating remission", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(InvalidPaymentGroupReferenceException.class)
    public String return404onInvalidPaymentGroupReference(InvalidPaymentGroupReferenceException ex) {
        LOG.error("Error while creating remission: {}", ex);
        return "Payment group reference not found";
    }
}
