package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.dto.OrganisationEntityResponse;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.IdamService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Api(tags = {"Pay By Account"})
@SwaggerDefinition(tags = {@Tag(name = "PBAController", description = "Pay by account REST API")})
public class PBAController {

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final PaymentDtoMapper paymentDtoMapper;

    @Autowired
    private IdamService idamService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired()
    @Qualifier("restTemplateRefData")
    private RestTemplate restTemplateRefData;

    private static final Logger LOG = LoggerFactory.getLogger(PBAController.class);

    public static final String RETRIEVE_PBA_ENDPOINT = "/refdata/external/v1/organisations/pbas";


    @Value("${auth.ref.data.baseUrl}")
    private String refDataBaseURL;

    @Autowired
    public PBAController(PaymentService<PaymentFeeLink, String> paymentService, PaymentDtoMapper paymentDtoMapper) {
        this.paymentService = paymentService;
        this.paymentDtoMapper = paymentDtoMapper;
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

    @ApiOperation(value = "Get PBA account details from ref data", notes = "Get PBA account details from ref data")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    @GetMapping(value = "/pba-accounts")
    @PaymentExternalAPI
    public ResponseEntity<OrganisationEntityResponse> retrievePBADetails(@RequestHeader(required = false) MultiValueMap<String, String> headers) {

        String userId = idamService.getUserId(headers);
        UserIdentityDataDto userIdentityDataDto = idamService.getUserIdentityData(headers, userId);
        String emailId = userIdentityDataDto.getEmailId();


        //Generate token for payment api and replace
        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        //Add email address retrieved from IDAM to invoke ref data end point
        List<String> email = new ArrayList<>();
        email.add(emailId);

        MultiValueMap<String, String> headerMultiValueMapForRefData = new LinkedMultiValueMap<String, String>();
        headerMultiValueMapForRefData.put("content-type", headers.get("content-type"));
        //User token
        headerMultiValueMapForRefData.put("Authorization", headers.get("authorization"));
        //Service token
        headerMultiValueMapForRefData.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        headerMultiValueMapForRefData.put("UserEmail", email);


        HttpHeaders headersForRefData = new HttpHeaders(headerMultiValueMapForRefData);
        final HttpEntity<String> entity = new HttpEntity<>(headersForRefData);

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(refDataBaseURL + RETRIEVE_PBA_ENDPOINT);
            LOG.debug("builder.toUriString() : {}", builder.toUriString());
            return restTemplateRefData
                .exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity, OrganisationEntityResponse.class
                );
    }
}
