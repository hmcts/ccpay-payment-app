package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchdarkly.shaded.org.apache.commons.codec.binary.Base64;
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
import java.util.stream.Collectors;

@Service
public class TestServiceNow  {
    private static final Logger LOG = LoggerFactory.getLogger(TestServiceNow.class);


    public static void main(String[] args) throws Exception{

        RestTemplate restTemplate = new RestTemplate();


        String iacServiceNowDevUrl="https://mojcppdev.service-now.com/api/now/table/incident?sysparm_fields=number";

        IacServiceNowRequest iacServiceNowRequest = IacServiceNowRequest.createIacServiceNowRequestWith().
            callerId("8918448e1bd1b8106248a822b24bcb14").
            contactType("Alert").
            //need to check
            serviceOffering("IAC - Appellant in Person").
            category("Alert").
            subcategory("Error").
            shortDescription("Immigration & Asylum Appeals Error + [File Name]").
            assignmentGroup("56b756774fbd368057db0b318110c7bd").
            description("[Error Exception] -- Testing").
            uRoleType("c319bc4bdb41834074abffa9bf96199c").
            impact("2").
            urgency("2").
            build();

        HttpHeaders headers = createHeaders("svc.feespay.rest.user","VWTD2zbp");
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity(iacServiceNowRequest, headers);
        ResponseEntity  iacServiceNowResponseEntity = restTemplate.exchange(iacServiceNowDevUrl, HttpMethod.POST, entity,IacServiceNowResponse.class);

        ObjectMapper objectMapperSupplementaryInfo = new ObjectMapper();

        IacServiceNowResponse iacServiceNowResponse = objectMapperSupplementaryInfo.convertValue(iacServiceNowResponseEntity.getBody(), IacServiceNowResponse.class);

        LOG.info("Incident has been raised against IAC : incident number : {}",iacServiceNowResponse.getResult().getNumber());

    }

    static HttpHeaders createHeaders(String username, String password){
        return new HttpHeaders() {{
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII")) );
            String authHeader = "Basic " + new String( encodedAuth );
            set( "Authorization", authHeader );
        }};
    }

}








