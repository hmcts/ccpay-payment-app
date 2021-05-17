package uk.gov.hmcts.payment.api.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import java.util.ArrayList;
import java.util.List;

@Service
public class IACServiceResponseImpl implements IACServiceResponse {
    private static final Logger LOG = LoggerFactory.getLogger(IACServiceResponseImpl.class);

    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;

    @Autowired
    @Qualifier("restTemplateIacSupplementaryInfo")
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Override
    @HystrixCommand(commandKey = "IACService", commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000"),
        @HystrixProperty(name = "fallback.enabled", value = "false")
    })
     public ResponseEntity<SupplementaryDetailsResponse> getIacSupplementaryInfoResponse(List<String> iacCcdCaseNos) throws RestClientException {

        IacSupplementaryRequest iacSupplementaryRequest = IacSupplementaryRequest.createIacSupplementaryRequestWith()
            .ccdCaseNumbers(iacCcdCaseNos).build();

        MultiValueMap<String, String> headerMultiValueMapForIacSuppInfo = new LinkedMultiValueMap<String, String>();
        List<String> serviceAuthTokenPaymentList = new ArrayList<>();

         //Generate token for payment api and replace
         serviceAuthTokenPaymentList.add(authTokenGenerator.generate());
         LOG.info("S2S Token To Test : {}", authTokenGenerator.generate());

        headerMultiValueMapForIacSuppInfo.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        LOG.info("IAC Supplementary info URL: {}", iacSupplementaryInfoUrl + "/supplementary-details");

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForIacSuppInfo);
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity<>(iacSupplementaryRequest, headers);
        return this.restTemplateIacSupplementaryInfo.exchange(iacSupplementaryInfoUrl + "/supplementary-details", HttpMethod.POST, entity, SupplementaryDetailsResponse.class);
    }

}








