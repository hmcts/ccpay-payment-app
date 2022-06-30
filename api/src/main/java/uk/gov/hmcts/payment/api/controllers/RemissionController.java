package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.mapstruct.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.RemissionDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.RemissionService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.*;

import javax.validation.Valid;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@Api(tags = {"Remissions"})
@SwaggerDefinition(tags = {@Tag(name = "RemissionController", description = "Remission REST API")})
public class RemissionController {
    private static final Logger LOG = LoggerFactory.getLogger(RemissionController.class);

    @Autowired
    private RemissionService remissionService;

    @Autowired
    private RemissionDtoMapper remissionDtoMapper;

    @Autowired
    private ReferenceDataService referenceDataService;

    @ApiOperation(value = "Create upfront remission record", notes = "Create upfront remission record")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed"),
        @ApiResponse(code = 404, message = "Given payment group reference not found"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/remissions")
    @ResponseBody
    public ResponseEntity<RemissionDto> createRemission(@Valid @RequestBody RemissionRequest remissionRequest,
                                                        @RequestHeader(required = false) MultiValueMap<String, String> headers)
        throws CheckDigitException {

        OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(Optional.ofNullable(remissionRequest.getCaseType()),Optional.empty(), headers);

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest, organisationalServiceDto);
        remissionRequest.getFee().setCcdCaseNumber(remissionRequest.getCcdCaseNumber());
        remissionServiceRequest.setFee(remissionDtoMapper.toFee(remissionRequest.getFee()));
        PaymentFeeLink paymentFeeLink = remissionService.createRemission(remissionServiceRequest);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Create retrospective remission record(not in use)", notes = "Create retrospective remission record")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed"),
        @ApiResponse(code = 404, message = "Given payment group reference not found"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/payment-groups/{payment-group-reference}/fees/{unique_fee_id}/remissions")
    @ResponseBody
    public ResponseEntity<RemissionDto> createRetrospectiveRemission(
        @PathVariable("payment-group-reference") String paymentGroupReference,
        @PathVariable("unique_fee_id") Integer feeId,
        @RequestHeader(required = false) MultiValueMap<String, String> headers,
        @Valid @RequestBody RemissionRequest remissionRequest) throws CheckDigitException {

        OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(Optional.ofNullable(remissionRequest.getCaseType()),Optional.empty(), headers);

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest, organisationalServiceDto);
        PaymentFeeLink paymentFeeLink = remissionService.createRetrospectiveRemission(remissionServiceRequest, paymentGroupReference, feeId);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Create retrospective remission record for payment", notes = "Create retrospective remission record for payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Retrospective Remission created"),
        @ApiResponse(code = 400, message = "Retrospective Remission creation failed"),
        @ApiResponse(code = 404, message = "Given payment group reference not found"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/payment-groups/{payment-group-reference}/fees/{unique_fee_id}/retro-remission")
    @ResponseBody
    public ResponseEntity<RetroRemissionDto> createRetrospectiveRemissionForPayment(
        @PathVariable("payment-group-reference") String paymentGroupReference,
        @PathVariable("unique_fee_id") Integer feeId,
        @RequestHeader(required = false) MultiValueMap<String, String> headers,
        @Valid @RequestBody RetroRemissionRequest retroRetroRemissionRequest) throws CheckDigitException {
        RetroRemissionServiceRequest remissionServiceRequest = populateRetroRemissionServiceRequest(retroRetroRemissionRequest);
        Remission remission = remissionService.createRetrospectiveRemissionForPayment(remissionServiceRequest, paymentGroupReference, feeId);
        RetroRemissionDto retroRemissionDto =  remissionDtoMapper.toCreateRetroRemissionResponse(remission);
        Link link = linkTo(RemissionController.class).slash("refund-retro-remisstion").withSelfRel();
        retroRemissionDto.add(link);
        return new ResponseEntity<>(retroRemissionDto, HttpStatus.CREATED);
    }

    private RemissionServiceRequest populateRemissionServiceRequest(RemissionRequest remissionRequest, OrganisationalServiceDto organisationalServiceDto) {
        return RemissionServiceRequest.remissionServiceRequestWith()
            .paymentGroupReference(PaymentReference.getInstance().getNext())
            .hwfAmount(remissionRequest.getHwfAmount())
            .hwfReference(remissionRequest.getHwfReference())
            .beneficiaryName(remissionRequest.getBeneficiaryName())
            .ccdCaseNumber(remissionRequest.getCcdCaseNumber())
            .caseReference(remissionRequest.getCaseReference())
            .siteId(organisationalServiceDto.getServiceCode())
            .build();
    }

    private RetroRemissionServiceRequest populateRetroRemissionServiceRequest(RetroRemissionRequest remissionRequest) {
        return RetroRemissionServiceRequest.retroRemissionServiceRequestWith()
            .hwfAmount(remissionRequest.getHwfAmount())
            .hwfReference(remissionRequest.getHwfReference())
            .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String return400onDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOG.error("Error while creating remission", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({InvalidPaymentGroupReferenceException.class, PaymentFeeNotFoundException.class})
    public String return404onInvalidPaymentGroupReference(PaymentException ex) {
        LOG.error("Error while creating remission: {}", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NoServiceFoundException.class})
    public String return404(NoServiceFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(GatewayTimeoutException.class)
    public String return504(GatewayTimeoutException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RemissionAlreadyExistException.class)
    public String return400RemissionCannotApply(RemissionAlreadyExistException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RemissionNotFoundException.class)
    public String return400RemissionNotFound(RemissionNotFoundException ex) {
        return ex.getMessage();
    }

}
