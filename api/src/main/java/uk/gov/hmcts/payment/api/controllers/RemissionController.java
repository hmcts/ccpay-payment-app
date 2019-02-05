package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.RemissionService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.validators.RemissionValidator;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Optional;

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
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private PaymentFeeRepository paymentFeeRepository;

    @ApiOperation(value = "Create remission record", notes = "Create remission record")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed")
    })
    @PostMapping(value = "/remission")
    @ResponseBody
    public ResponseEntity<String> createRemission(@Valid @RequestBody RemissionRequest remissionRequest)
        throws CheckDigitException {
        remissionValidator.validate(remissionRequest);
        Optional<FeeDto> feeDto = Optional.ofNullable(remissionRequest.getFee());
        //FeeDto feeDto = remissionRequest.getFee();
        PaymentFee paymentFee = feeDto.map(feeDto1 -> PaymentFee.feeWith()
            .code(feeDto1.getCode())
            .version(feeDto1.getVersion())
            .volume(feeDto1.getVolume())
            .calculatedAmount(feeDto1.getCalculatedAmount())
            .ccdCaseNumber(feeDto1.getCcdCaseNumber())
            .reference(feeDto1.getReference())
            .build()).orElse(null);

        if (StringUtils.isEmpty(remissionRequest.getPaymentGroupReference())) {
            String paymentGroupReference = PaymentReference.getInstance().getNext();
            remissionRequest.setPaymentGroupReference(paymentGroupReference);
            PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
                .paymentReference(paymentGroupReference)
                .fees(paymentFee != null ? Collections.singletonList(paymentFee) : null)
                .build();

            paymentFeeLinkRepository.save(paymentFeeLink);
        } else if (feeDto.isPresent()) {
            PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository
                .findByPaymentReference(remissionRequest.getPaymentGroupReference())
                .orElseThrow(InvalidPaymentGroupReferenceException::new);

            paymentFee.setPaymentLink(paymentFeeLink);

            paymentFeeRepository.save(paymentFee);
        }

        String generatedRemissionReference = remissionService.create(remissionRequest.toRemission());

        return new ResponseEntity<>(generatedRemissionReference, HttpStatus.CREATED);
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
}
