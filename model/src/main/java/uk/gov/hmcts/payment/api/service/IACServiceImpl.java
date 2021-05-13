package uk.gov.hmcts.payment.api.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.IacSupplementaryRequest;
import uk.gov.hmcts.payment.api.dto.SupplementaryDetailsResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class IACServiceImpl implements IACService {
    private static final Logger LOG = LoggerFactory.getLogger(IACServiceImpl.class);

    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;

    @Autowired
    @Qualifier("restTemplateIacSupplementaryInfo")
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Override
    @HystrixCommand(commandKey = "getIacSupplementaryInfo", commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000"),
        @HystrixProperty(name = "fallback.enabled", value = "false")
    })
    public ResponseEntity<SupplementaryDetailsResponse> getIacSupplementaryInfo(List<String> iacCcdCaseNos,String serviceToken) {

        IacSupplementaryRequest iacSupplementaryRequest = IacSupplementaryRequest.createIacSupplementaryRequestWith()
            .ccdCaseNumbers(iacCcdCaseNos).build();

        MultiValueMap<String, String> headerMultiValueMapForIacSuppInfo = new LinkedMultiValueMap<String, String>();

        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        //Generate token for payment api and replace
        serviceAuthTokenPaymentList.add(serviceToken);
        LOG.info("S2S Token To Test : {}", serviceToken);


        headerMultiValueMapForIacSuppInfo.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        serviceAuthTokenPaymentList=null;
        LOG.info("IAC Supplementary info URL: {}",iacSupplementaryInfoUrl+"/supplementary-details");

        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForIacSuppInfo);
        headerMultiValueMapForIacSuppInfo=null;
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity<>(iacSupplementaryRequest, headers);
        headers=null;
        return restTemplateIacSupplementaryInfo.exchange(iacSupplementaryInfoUrl + "/supplementary-details", HttpMethod.POST, entity, SupplementaryDetailsResponse.class);
    }
}


