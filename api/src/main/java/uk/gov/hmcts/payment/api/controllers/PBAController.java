package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.PBAResponse;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.IdamService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Api(tags = {"Pay By Account"})
@SwaggerDefinition(tags = {@Tag(name = "PBAController", description = "Pay by account REST API")})
public class PBAController {

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final PaymentDtoMapper paymentDtoMapper;

    private final IdamService idamService;

    private final AuthTokenGenerator authTokenGenerator;

    @Autowired()
    @Qualifier("restTemplateRefData")
    private RestTemplate restTemplateRefData;

    private static final Logger LOG = LoggerFactory.getLogger(PBAController.class);

    public static final String RETRIEVE_PBA_ENDPOINT = "/refdata/external/v1/organisations/pbas";


    @Value("${auth.ref.data.baseUrl}")
    private String refDataBaseURL;

    @Autowired
    ObjectMapper pbaObjectMapper;

    @Autowired
    public PBAController(PaymentService<PaymentFeeLink, String> paymentService, PaymentDtoMapper paymentDtoMapper, IdamService idamService, AuthTokenGenerator authTokenGenerator) {
        this.paymentService = paymentService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.idamService = idamService;
        this.authTokenGenerator = authTokenGenerator;
    }

    @ApiOperation(value = "Get payments for a PBA account", notes = "Get list of payments")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(value = "/pba-accounts/{account}/payments")
    @PaymentExternalAPI
    public PaymentsResponse retrievePaymentsByAccount(@PathVariable(name = "account") String account) {

        List<PaymentFeeLink> paymentFeeLinks = paymentService.search(PaymentSearchCriteria.searchCriteriaWith().pbaNumber(account).build());

        List<PaymentDto> paymentDto = paymentFeeLinks.stream()
            .map(paymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        return new PaymentsResponse(paymentDto);
    }

    @ApiOperation(value = "Get PBA account details from ref data", notes = "Get list of PBA account details from ref data")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "PBA accounts retrieved"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "No PBA Accounts found.")
    })
    @GetMapping(value = "/pba-accounts")
    public ResponseEntity<PBAResponse> retrievePBADetails(@RequestHeader(required = false) MultiValueMap<String, String> headers) {

        String emailIdFromIdam = idamService.getUserId(headers);

        MultiValueMap<String, String> headerMultiValueMapForRefData = generateHeaders(headers, emailIdFromIdam);

        try {
            ResponseEntity<PBAResponse> response = getDetailsFromRefData(headerMultiValueMapForRefData);
            PBAResponse pbaResponse = pbaObjectMapper.convertValue(response.getBody(), PBAResponse.class);
            return new ResponseEntity(pbaResponse, HttpStatus.OK);
        } catch (HttpClientErrorException httpClientErrorException) {
            LOG.info("Exception : {} ",httpClientErrorException.getStatusCode());
            if(httpClientErrorException.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new AccountNotFoundException("No PBA Accounts found"); }
            else {
                throw new AccountServiceUnavailableException(httpClientErrorException.getMessage());
            }
        }
        catch (Exception exception) {
            throw new PaymentException(exception.getMessage());
        }
    }

    private MultiValueMap<String, String> generateHeaders(MultiValueMap<String, String> headers, String emailId) {
        //Generate token for payment api and replace
        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        //Add email address retrieved from IDAM to invoke ref data end point
        List<String> email = new ArrayList<>();
        email.add(emailId);

        MultiValueMap<String, String> headerMultiValueMapForRefData = new LinkedMultiValueMap<String, String>();
        headerMultiValueMapForRefData.put("Content-Type", List.of("application/json"));
        //User token

        String userAuthorization = headers.get("authorization") == null ? headers.get("Authorization").get(0) : headers.get(
            "authorization").get(0);
        headerMultiValueMapForRefData.put(
            "Authorization", Collections.singletonList(userAuthorization.startsWith("Bearer ")
                ? userAuthorization : "Bearer ".concat(userAuthorization))
        );
        //Service token
        headerMultiValueMapForRefData.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        headerMultiValueMapForRefData.put("UserEmail", email);
        LOG.info("headers to ref data : {}", headerMultiValueMapForRefData);
        LOG.info("ServiceAuthorization Token : {}", serviceAuthTokenPaymentList);
        return headerMultiValueMapForRefData;
    }

    private ResponseEntity<PBAResponse> getDetailsFromRefData(MultiValueMap<String, String> headerMultiValueMapForRefData) {
        HttpHeaders headersForRefData = new HttpHeaders(headerMultiValueMapForRefData);
        final HttpEntity<String> entity = new HttpEntity<>(headersForRefData);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refDataBaseURL+RETRIEVE_PBA_ENDPOINT);
        LOG.info("builder.toUriString() : {}", builder.toUriString());
        return restTemplateRefData
            .exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity, PBAResponse.class
            );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public String return404(AccountNotFoundException ex) {
        LOG.error("No PBA Accounts found:", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler( AccountServiceUnavailableException.class)
    public String return500( AccountServiceUnavailableException ex) {
        LOG.error("Internal Server Error :", ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        LOG.error("Error while processing payment request:", ex);
        return ex.getMessage();
    }
}
