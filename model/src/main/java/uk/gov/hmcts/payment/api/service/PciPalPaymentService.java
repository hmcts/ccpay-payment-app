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
import uk.gov.hmcts.payment.api.external.client.dto.PCIPALAntennaLinkIdResponse;
import uk.gov.hmcts.payment.api.external.client.dto.PCIPALAntennaRequest;
import uk.gov.hmcts.payment.api.external.client.dto.PCIPALAntennaResponse;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;


@Service
public class PciPalPaymentService implements DelegatingPaymentService<PciPalPayment, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PciPalPaymentService.class);
    private static final String SERVICE_TYPE_PROBATE = "probate";
    private static final String SERVICE_TYPE_CMC = "cmc";
    private static final String SERVICE_TYPE_DIVORCE = "divorce";
    private static final String SERVICE_TYPE_FINREM = "finrem";
    @Value("${pci-pal.account.id.cmc}")
    private String ppAccountIDCmc;
    @Value("${pci-pal.account.id.probate}")
    private String ppAccountIDProbate;
    @Value("${pci-pal.account.id.divorce}")
    private String ppAccountIDDivorce;
    @Value("${pci-pal.account.id.finrem}")
    private String ppAccountIDFinrem;

    @Value("${pci-pal.antenna.grant.type}")
    private String grantType;

    @Value("${pci-pal.antenna.tenant.name}")
    private String tenantName;

    @Value("${pci-pal.antenna.user.name}")
    private String userName;

    @Value("${pci-pal.antenna.client.id}")
    private String clientId;

    @Value("${pci-pal.antenna.client.secret}")
    private String clientSecret;

    @Value("${pci-pal.antenna.get.tokens.url}")
    private String tokensURL;

    @Value("${pci-pal.antenna.launch.url}")
    private String launchURL;

    @Value("${pci-pal.antenna.view.id.url}")
    private String viewIdURL;

    @Value("${pci-pal.antenna.return.url}")
    private String returnURL;

    @Value("${pci-pal.antenna.cmc.flow.id}")
    private String cmcFlowId;

    @Value("${pci-pal.antenna.probate.flow.id")
    private String probateFlowId;

    @Value("${pci-pal.antenna.divorce.flow.id}")
    private String divorceFlowId;

    @Value("${pci-pal.antenna.financial.remedy.flow.id}")
    private String financialRemedyFlowId;


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
            String ppAccountID = getppAccountId(serviceType);

            LOG.info("ppAccountID: {} SERVICE_TYPE_CMC: {} serviceType: {}", ppAccountID, SERVICE_TYPE_CMC, serviceType);
            List<NameValuePair> params = new ArrayList<>();
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

    private String getppAccountId(String serviceType) {
        String ppAccountID = null;
        if (serviceType.equalsIgnoreCase(SERVICE_TYPE_DIVORCE)) {
            ppAccountID = ppAccountIDDivorce;
        }
        else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_CMC)) {
            ppAccountID = ppAccountIDCmc;
        }
        else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_PROBATE)) {
            ppAccountID = ppAccountIDProbate;
        }
        else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_FINREM)) {
            ppAccountID = ppAccountIDFinrem;
        }
        else
        {
            throw new PaymentException("Invalid service type: " + serviceType);
        }
        return ppAccountID;
    }

    public PCIPALAntennaResponse getPciPalAntennaLink(PciPalPaymentRequest pciPalPaymentRequest, PCIPALAntennaResponse pcipalAntennaResponse,String serviceType) {
        return withIOExceptionHandling(() -> {

            String flowId = getFlowId(serviceType);
            LOG.info("flowId: {} launchURL: {} viewIdURL: {} callbackUrl: {} returnURL: {} ", flowId, launchURL, viewIdURL, callbackUrl, returnURL);
            HttpPost httpPost = new HttpPost(launchURL);
            httpPost.addHeader(CONTENT_TYPE, APPLICATION_JSON.toString());
            httpPost.addHeader(authorizationHeader(pcipalAntennaResponse.getAccessToken()));
            PCIPALAntennaRequest pcipalAntennaRequest = PCIPALAntennaRequest.pciPALAntennaRequestWith().FlowId(flowId)
                .InitialValues(PCIPALAntennaRequest.InitialValues.initialValuesWith()
                    .amount(new BigDecimal(pciPalPaymentRequest.getOrderAmount()).movePointRight(2).toString())
                    .callbackURL(callbackUrl)
                    .returnURL(returnURL)
                    .orderId(pciPalPaymentRequest.getOrderReference())
                    .currencyCode("GBP")
                    .build())
                .build();

            Gson gson = new Gson();
            StringEntity entity = new StringEntity(gson.toJson(pcipalAntennaRequest));
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            PCIPALAntennaLinkIdResponse pcipalAntennaLinkIdResponse = objectMapper.readValue(response.getEntity().getContent(), PCIPALAntennaLinkIdResponse.class);
            pcipalAntennaResponse.setNextUrl(viewIdURL + pcipalAntennaLinkIdResponse.getId()+"/framed");

            return pcipalAntennaResponse;
        });
    }

    private String getFlowId(String serviceType) {
        String flowId = null;
        if (serviceType.equalsIgnoreCase(SERVICE_TYPE_DIVORCE)) {
            flowId = divorceFlowId;
        }
        else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_CMC)) {
            flowId = cmcFlowId;
        }
        else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_PROBATE)) {
            flowId = probateFlowId;
        }

        else if (serviceType.equalsIgnoreCase(SERVICE_TYPE_FINREM)) {
            flowId = financialRemedyFlowId;
        }
        else
        {
            throw new PaymentException("Invalid service type: " + serviceType);
        }
        return flowId;
    }

    public PCIPALAntennaResponse getPciPalTokens() {
        return withIOExceptionHandling(() -> {
            LOG.info("grant_type: {} tenantname: {} username: {} client_id: {} client_secret: {} tokensURL: {}", grantType, tenantName, userName, clientId, clientSecret, tokensURL);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", grantType));
            params.add(new BasicNameValuePair("tenantname", tenantName));
            params.add(new BasicNameValuePair("username", userName));
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));

            HttpPost httpPost = new HttpPost(tokensURL);
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response1 = httpClient.execute(httpPost);

            return objectMapper.readValue(response1.getEntity().getContent(), PCIPALAntennaResponse.class);
        });
    }
    private Header authorizationHeader(String authorizationKey) {
        return new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationKey);
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
