package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.exceptions.PciPalClientException;
import uk.gov.hmcts.payment.api.external.client.dto.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;


@Service
public class PciPalPaymentService implements DelegatingPaymentService<PciPalPayment, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PciPalPaymentService.class);
    private static final String SERVICE_TYPE_PROBATE = "probate";
    private static final String SERVICE_TYPE_CMC = "cmc";
    private static final String SERVICE_TYPE_DIVORCE = "divorce";
    @Value("${pci-pal.account.id.cmc}")
    private String ppAccountIDCmc;
    @Value("${pci-pal.account.id.probate}")
    private String ppAccountIDProbate;
    @Value("${pci-pal.account.id.divorce}")
    private String ppAccountIDDivorce;

    private final String callbackUrl;
    private final String url;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public PciPalPaymentService(@Value("${pci-pal.api.url}") String url,
                                @Value("${pci-pal.callback-url}") String callbackUrl,HttpClient httpClient,ObjectMapper objectMapper) {
        this.url = url;
        this.callbackUrl = callbackUrl;
        this.httpClient= httpClient;
        this.objectMapper= objectMapper;
    }

    public String getPciPalLink(PciPalPaymentRequest pciPalPaymentRequest, String serviceType) {
        LOG.debug("CMC: {} DIVORCE: {} PROBATE: {}", ppAccountIDCmc, ppAccountIDDivorce, ppAccountIDProbate);
        return withIOExceptionHandling(() -> {
            String ppAccountID = null;
            if (serviceType.equalsIgnoreCase(SERVICE_TYPE_DIVORCE))
                ppAccountID = ppAccountIDDivorce;
            else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_CMC))
                ppAccountID = ppAccountIDCmc;
            else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_PROBATE))
                ppAccountID = ppAccountIDProbate;

            LOG.debug("ppAccountID: {} SERVICE_TYPE_CMC: {} serviceType: {}", ppAccountID, SERVICE_TYPE_CMC, serviceType);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("ppAccountID", ppAccountID));
            params.add(new BasicNameValuePair("orderAmount", new BigDecimal(pciPalPaymentRequest.getOrderAmount()).movePointRight(2).toString()));
            params.add(new BasicNameValuePair("orderReference", pciPalPaymentRequest.getOrderReference()));
            params.add(new BasicNameValuePair("callbackURL", callbackUrl));
            params.add(new BasicNameValuePair("customData2", pciPalPaymentRequest.getCustomData2()));


            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameters(params);
            HttpGet request = new HttpGet(uriBuilder.build());

            return request.getURI().toString();
        });
    }

    public String getPciPalAntennaLink(PciPalPaymentRequest pciPalPaymentRequest, String serviceType) {
        LOG.debug("CMC: {} DIVORCE: {} PROBATE: {}", ppAccountIDCmc, ppAccountIDDivorce, ppAccountIDProbate);
        return withIOExceptionHandling(() -> {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("grant_type", "client_credentials"));
            params.add(new BasicNameValuePair("tenantname", "PCI Pal Test Integration"));
            params.add(new BasicNameValuePair("username", "HMCTS_user"));
            params.add(new BasicNameValuePair("client_id", "HMCTStest"));
            params.add(new BasicNameValuePair("client_secret", "469Q4RblXA5atSI1U8pFW3AQZqvYjwD9B7XUp47c"));

            HttpPost httpPost1 = new HttpPost("https://pcipalstaging.cloud/api/v1/token");
            httpPost1.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response1 = httpClient.execute(httpPost1);
            //String responseBody = EntityUtils.toString(response.getEntity());

            PCIPALAntennaResponse pcipalAntennaResponse = objectMapper.readValue(response1.getEntity().getContent(), PCIPALAntennaResponse.class);
            System.out.println(pcipalAntennaResponse);

            //PCI PAL 2nd call

            HttpPost httpPost2 = new HttpPost("https://euwest1.pcipalstaging.cloud/api/v1/session/319/launch");
            httpPost2.addHeader(CONTENT_TYPE, APPLICATION_JSON.toString());
            httpPost2.addHeader(authorizationHeader(pcipalAntennaResponse.getAccessToken()));
            PCIPALAntennaRequest pcipalAntennaRequest = PCIPALAntennaRequest.pciPALAntennaRequestWith().FlowId("1201")
                .InitialValues(PCIPALAntennaRequest.InitialValues.initialValuesWith()
                    .Amount(new BigDecimal(pciPalPaymentRequest.getOrderAmount()).movePointRight(2).toString())
                    .CallbackURL(callbackUrl)
                    .RedirectURL("http://localhost")
                    .Reference(pciPalPaymentRequest.getOrderReference())
                    .build())
                    .build();
            //String json = objectMapper.writeValueAsString(pcipalAntennaRequest);

            Gson gson = new Gson();
            String json = gson.toJson(pcipalAntennaRequest);
            System.out.println(json);
            StringEntity entity = new StringEntity(json);
            httpPost2.setEntity(entity);
            HttpResponse response2 = httpClient.execute(httpPost2);
            //String responseBody = EntityUtils.toString(response2.getEntity());
            PCIPALAntennaResponse2 pcipalAntennaResponse2 = objectMapper.readValue(response2.getEntity().getContent(), PCIPALAntennaResponse2.class);
            System.out.println(pcipalAntennaResponse2);

            //PCI PAL 3rd call

            String uri = "https://euwest1.pcipalstaging.cloud/session/319/view/";
            String finalUri = uri + pcipalAntennaResponse2.getId();
            HttpPost httpPost3 = new HttpPost(finalUri);
            httpPost3.addHeader(CONTENT_TYPE, APPLICATION_FORM_URLENCODED.toString());
           // httpPost3.addHeader("X-BEARER-TOKEN",authorizationHeaderString(pcipalAntennaResponse.getAccessToken()));
            //httpPost3.addHeader("X-REFRESH-TOKEN",pcipalAntennaResponse.getRefreshToken());


            List<NameValuePair> params1 = new ArrayList<NameValuePair>();
            params1.add(new BasicNameValuePair("X-BEARER-TOKEN", pcipalAntennaResponse.getAccessToken()));
            params1.add(new BasicNameValuePair("X-REFRESH-TOKEN", pcipalAntennaResponse.getRefreshToken()));
            httpPost3.setEntity(new UrlEncodedFormEntity(params1));
            HttpResponse response3 = httpClient.execute(httpPost3);
            String responseBody = EntityUtils.toString(response3.getEntity());
            System.out.println(responseBody);
            return httpPost1.getURI().toString();
        });
    }

    private Header authorizationHeader(String authorizationKey) {
        return new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationKey);
    }

    private String authorizationHeaderString(String authorizationKey) {
        return "Bearer " + authorizationKey;
    }

    private <T> T withIOExceptionHandling(CheckedExceptionProvider<T> provider) {
        try {
            return provider.get();
        } catch (IOException | URISyntaxException e) {
            throw new PciPalClientException(e);
        }
    }

    @Override
    public PciPalPayment create(PaymentServiceRequest paymentServiceRequest) {
        PciPalPayment payment = PciPalPayment.pciPalPaymentWith().paymentId("spoof_id")
            .state(State.stateWith().code("code").finished(false).message("message").status("created").build()).build();
        LOG.info("PciPal service called, returning with: {}", payment);
        return payment;
    }

    @Override
    public PciPalPayment update(PaymentServiceRequest paymentServiceRequest) {
        return null;
    }

    @Override
    public PciPalPayment retrieve(String s) {
        return null;
    }

    @Override
    public PciPalPayment retrieve(String s, String paymentTargetService) {
        return null;
    }

    @Override
    public List<PciPalPayment> search(PaymentSearchCriteria searchCriteria) {
        return null;
    }

    private interface CheckedExceptionProvider<T> {
        T get() throws IOException, URISyntaxException;
    }

    @Override
    public void cancel(String paymentReference) {}

}
