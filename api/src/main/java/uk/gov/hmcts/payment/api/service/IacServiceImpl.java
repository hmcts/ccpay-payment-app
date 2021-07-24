package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IacServiceImpl implements IacService {
    private static final Logger LOG = LoggerFactory.getLogger(IacServiceImpl.class);


    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;

    @Autowired
    @Qualifier("restTemplateIacSupplementaryInfo")
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Value("${servicenow.username}")
    private String servicenowUsername;

    @Value("${servicenow.password}")
    private String servicenowPassword;

    @Value("${servicenow.url}")
    private String servicenowUrl;

    @Value("${servicenow.callerId}")
    private String servicenowCallerId;

    @Value("${servicenow.contactType}")
    private String servicenowContactType;

    @Value("${servicenow.serviceOffering}")
    private String servicenowServiceOffering;

    @Value("${servicenow.category}")
    private String servicenowCategory;

    @Value("${servicenow.subcategory}")
    private String servicenowSubCategory;

    @Value("${servicenow.shortDescription}")
    private String servicenowShortDescription;

    @Value("${servicenow.assignmentGroup}")
    private String servicenowAssignmentGroup;

    @Value("${servicenow.uRoleType}")
    private String servicenowURoleType;

    @Value("${servicenow.impact}")
    private String servicenowImpact;

    @Value("${servicenow.urgency}")
    private String servicenowUrgency;

    @Override
    public ResponseEntity<SupplementaryPaymentDto> getIacSupplementaryInfo(List<PaymentDto> paymentDtos, String serviceName) {
       HttpStatus paymentResponseHttpStatus = HttpStatus.OK;
        boolean CanRaiseServicenowIncident = false;
        List<PaymentDto> iacPayments = paymentDtos.stream().filter(payment -> (payment.getServiceName().equalsIgnoreCase(serviceName))).
            collect(Collectors.toList());
        LOG.info("No of Iac payment retrieved  : {}", iacPayments.size());

        Set<String> iacCcdCaseNos = iacPayments.stream().map(paymentDto -> paymentDto.getCcdCaseNumber()).
            collect(Collectors.toSet());

        ResponseEntity<SupplementaryDetailsResponse> responseEntitySupplementaryInfo = null;

        List<SupplementaryInfo> lstSupplementaryInfo = null;
        SupplementaryPaymentDto supplementaryPaymentDto = null;
        String servicenowErrorDetail = null;

        if (!iacCcdCaseNos.isEmpty()) {
            LOG.info("List of IAC Ccd Case numbers : {}", iacCcdCaseNos.toString());
            try {
                responseEntitySupplementaryInfo = getIacSupplementaryInfoResponse(iacCcdCaseNos);
            } catch (HttpClientErrorException ex) {
                LOG.info("IAC Supplementary information could not be found for the list of Ccd Case numbers : {} , Exception: {}", iacCcdCaseNos.toString(), ex.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                servicenowErrorDetail = "IAC Supplementary information could not be found for the list of Ccd Case numbers : " + iacCcdCaseNos.toString() + " , Exception:" +  ex.getMessage();
                CanRaiseServicenowIncident = true;
            } catch (Exception ex) {
                LOG.info("Unable to retrieve IAC Supplementary Info information for the list of Ccd Case numbers : {}, Exception: {}", iacCcdCaseNos.toString(),ex.getMessage());
                paymentResponseHttpStatus = HttpStatus.PARTIAL_CONTENT;
                servicenowErrorDetail ="Unable to retrieve IAC Supplementary Info information for the list of Ccd Case numbers :" +  iacCcdCaseNos.toString() + ", Exception: {}"+ ex.getMessage();
                CanRaiseServicenowIncident = true;
            }
            if (!CanRaiseServicenowIncident) {
                paymentResponseHttpStatus = responseEntitySupplementaryInfo.getStatusCode();
                ObjectMapper objectMapperSupplementaryInfo = new ObjectMapper();
                SupplementaryDetailsResponse supplementaryDetailsResponse = objectMapperSupplementaryInfo.convertValue(responseEntitySupplementaryInfo.getBody(), SupplementaryDetailsResponse.class);
                lstSupplementaryInfo = supplementaryDetailsResponse.getSupplementaryInfo();
                MissingSupplementaryInfo lstMissingSupplementaryInfo = supplementaryDetailsResponse.getMissingSupplementaryInfo();

                if (responseEntitySupplementaryInfo.getStatusCodeValue() == HttpStatus.PARTIAL_CONTENT.value() && lstMissingSupplementaryInfo == null) {
                    LOG.info("No missing supplementary info received from IAC for any of the Ccd case numbers , requested list of Ccd numbers : " + iacCcdCaseNos.toString() + ", however response is 206");
                    servicenowErrorDetail = "No missing supplementary info received from IAC for any of the Ccd case numbers,  requested list of Ccd numbers : " + iacCcdCaseNos.toString() +  ", however response is 206";
                    CanRaiseServicenowIncident = true;
                } else if (lstMissingSupplementaryInfo != null && lstMissingSupplementaryInfo.getCcdCaseNumbers() != null) {
                    LOG.info("missing supplementary info from IAC for CCD case numbers : {}", lstMissingSupplementaryInfo.getCcdCaseNumbers().toString());
                    servicenowErrorDetail = "missing supplementary info from IAC for CCD case numbers : " + lstMissingSupplementaryInfo.getCcdCaseNumbers().toString();
                    CanRaiseServicenowIncident = true;
                }
            }
            if(CanRaiseServicenowIncident) {
                raiseServicenowIncident(servicenowErrorDetail);
            }
            supplementaryPaymentDto = SupplementaryPaymentDto.supplementaryPaymentDtoWith().payments(paymentDtos).
                supplementaryInfo(lstSupplementaryInfo).build();
            }
           return new ResponseEntity(supplementaryPaymentDto, paymentResponseHttpStatus);
    }

    private ResponseEntity<SupplementaryDetailsResponse> getIacSupplementaryInfoResponse(Set<String> iacCcdCaseNos) throws RestClientException {

        IacSupplementaryRequest iacSupplementaryRequest = IacSupplementaryRequest.createIacSupplementaryRequestWith()
            .ccdCaseNumbers(iacCcdCaseNos).build();

        MultiValueMap<String, String> headerMultiValueMapForIacSuppInfo = new LinkedMultiValueMap<String, String>();
        List<String> serviceAuthTokenPaymentList = new ArrayList<>();

        //Generate token for payment api and replace
        serviceAuthTokenPaymentList.add(authTokenGenerator.generate());

        headerMultiValueMapForIacSuppInfo.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        LOG.info("IAC Supplementary info URL: {}", iacSupplementaryInfoUrl + "/supplementary-details");

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForIacSuppInfo);
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity<>(iacSupplementaryRequest, headers);
        return this.restTemplateIacSupplementaryInfo.exchange(iacSupplementaryInfoUrl + "/supplementary-details", HttpMethod.POST, entity, SupplementaryDetailsResponse.class);
    }

    private void raiseServicenowIncident(String serviceNowErrorDetail) {
        try {
            IacServiceNowRequest iacServiceNowRequest = IacServiceNowRequest.createIacServiceNowRequestWith().
            callerId(servicenowCallerId).
            contactType(servicenowContactType).
             //need to check -- will need the details
            serviceOffering(servicenowServiceOffering).
            category(servicenowCategory).
            subcategory(servicenowSubCategory).
            shortDescription(servicenowShortDescription).
            assignmentGroup(servicenowAssignmentGroup).
            description(serviceNowErrorDetail).
            uRoleType(servicenowURoleType).
            impact(servicenowImpact).
            urgency(servicenowUrgency).
            build();

            LOG.info("servicenowUrl : {}" , servicenowUrl);
            LOG.info("servicenowUsername : {}", servicenowUsername);
            LOG.info("servicenowPassword : {}", servicenowPassword);
            HttpHeaders headers = createHeaders(servicenowUsername, servicenowPassword);
            final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity(iacServiceNowRequest, headers);
            ResponseEntity iacServiceNowResponseEntity = this.restTemplateIacSupplementaryInfo.exchange(servicenowUrl, HttpMethod.POST, entity, IacServiceNowResponse.class);
            ObjectMapper objectMapperSupplementaryInfo = new ObjectMapper();
            IacServiceNowResponse iacServiceNowResponse = objectMapperSupplementaryInfo.convertValue(iacServiceNowResponseEntity.getBody(), IacServiceNowResponse.class);
            LOG.info("ServiceNow incident has been raised against IAC : incident number : {}", iacServiceNowResponse.getResult().getNumber());
        }catch(Exception ex){
            LOG.info("Unable to raise the ServiceNow incident against IAC , Exception : {}",ex.getMessage());
        }
    }

    HttpHeaders createHeaders(String username, String password){
        return new HttpHeaders() {{
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII")) );
            String authHeader = "Basic " + new String( encodedAuth );
            set( "Authorization", authHeader );
        }};
    }

}








