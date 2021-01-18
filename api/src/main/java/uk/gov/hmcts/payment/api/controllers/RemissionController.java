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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.dto.RemissionRequest;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.mapper.RemissionDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.RemissionService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentFeeNotFoundException;
import uk.gov.hmcts.payment.api.validators.RemissionValidator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@Api(tags = {"Remissions"})
@SwaggerDefinition(tags = {@Tag(name = "RemissionController", description = "Remission REST API")})
public class RemissionController {
    private static final Logger LOG = LoggerFactory.getLogger(RemissionController.class);

    @Autowired
    private RemissionService remissionService;

    @Autowired
    private RemissionValidator remissionValidator;

    @Autowired
    private RemissionDtoMapper remissionDtoMapper;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private ReferenceDataService referenceDataService;


    @ApiOperation(value = "Create upfront/retrospective remission record", notes = "Create upfront/retrospective remission record - Tactical")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Remission created"),
        @ApiResponse(code = 400, message = "Remission creation failed"),
        @ApiResponse(code = 404, message = "Given payment group reference not found"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/remission")
    @ResponseBody
    @Deprecated
    public ResponseEntity<RemissionDto> createRemissionV1(@Valid @RequestBody RemissionRequest remissionRequest,
                                                          @RequestHeader(required = false) MultiValueMap<String, String> headers)
        throws CheckDigitException {

        getOrganisationalDetails(headers, remissionRequest);

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest);
        remissionRequest.getFee().setCcdCaseNumber(remissionRequest.getCcdCaseNumber());
        remissionServiceRequest.setFee(remissionDtoMapper.toFee(remissionRequest.getFee()));
        PaymentFeeLink paymentFeeLink = remissionRequest.getPaymentGroupReference() == null ?
            remissionService.createRemission(remissionServiceRequest) :
            remissionService.createRetrospectiveRemission(remissionServiceRequest, remissionRequest.getPaymentGroupReference(), null);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }


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

        getOrganisationalDetails(headers, remissionRequest);

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest);
        remissionRequest.getFee().setCcdCaseNumber(remissionRequest.getCcdCaseNumber());
        remissionServiceRequest.setFee(remissionDtoMapper.toFee(remissionRequest.getFee()));
        PaymentFeeLink paymentFeeLink = remissionService.createRemission(remissionServiceRequest);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Create retrospective remission record", notes = "Create retrospective remission record")
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

        LOG.info("Case Type: {} ", remissionRequest.getCaseType());

        if(StringUtils.isNotBlank(remissionRequest.getCaseType())) {
            getOrganisationalDetails(headers, remissionRequest);
        }
        LOG.info("SiteId : {} ", remissionRequest.getSiteId());

        RemissionServiceRequest remissionServiceRequest = populateRemissionServiceRequest(remissionRequest);
        PaymentFeeLink paymentFeeLink = remissionService.createRetrospectiveRemission(remissionServiceRequest, paymentGroupReference, feeId);

        return new ResponseEntity<>(remissionDtoMapper.toCreateRemissionResponse(paymentFeeLink), HttpStatus.CREATED);
    }

    private void getOrganisationalDetails(MultiValueMap<String, String> headers, RemissionRequest remissionRequest) {
        try {
            List<String> serviceAuthTokenPaymentList = new ArrayList<>();

            MultiValueMap<String, String> headerMultiValueMapForOrganisationalDetail = new LinkedMultiValueMap<String, String>();
            serviceAuthTokenPaymentList.add(authTokenGenerator.generate());
            LOG.info("Service Token : {}", serviceAuthTokenPaymentList);
            headerMultiValueMapForOrganisationalDetail.put("Content-Type", headers.get("content-type"));
            //User token
            headerMultiValueMapForOrganisationalDetail.put("Authorization", Collections.singletonList("Bearer " + headers.get("authorization")));
            //Service token
            headerMultiValueMapForOrganisationalDetail.put("ServiceAuthorization", serviceAuthTokenPaymentList);
            //Http headers
            HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMapForOrganisationalDetail);
            final HttpEntity<String> entity = new HttpEntity<>(headers);

            OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(remissionRequest.getCaseType(), entity);
            remissionRequest.setSiteId(organisationalServiceDto.getServiceCode());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOG.error("ORG ID Ref error status Code {} ", e.getRawStatusCode());
            if (e.getRawStatusCode() == 404) {
                throw new NoServiceFoundException("No Service found for given CaseType");
            }
            if (e.getRawStatusCode() == 504) {
                throw new GatewayTimeoutException("Unable to retrieve service information. Please try again later");
            }
        }
    }

    private RemissionServiceRequest populateRemissionServiceRequest(RemissionRequest remissionRequest) {
        return RemissionServiceRequest.remissionServiceRequestWith()
            .paymentGroupReference(PaymentReference.getInstance().getNext())
            .hwfAmount(remissionRequest.getHwfAmount())
            .hwfReference(remissionRequest.getHwfReference())
            .beneficiaryName(remissionRequest.getBeneficiaryName())
            .ccdCaseNumber(remissionRequest.getCcdCaseNumber())
            .caseReference(remissionRequest.getCaseReference())
            .siteId(remissionRequest.getSiteId())
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
}
