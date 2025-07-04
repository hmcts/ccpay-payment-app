package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicNameValuePair;
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
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PciPalConfigurationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;


@Service
public class PciPalPaymentService implements DelegatingPaymentService<PciPalPayment, String> {
    private static final Logger LOG = LoggerFactory.getLogger(PciPalPaymentService.class);
    private static final String SERVICE_TYPE_PROBATE = "Probate";
    private static final String SERVICE_TYPE_CMC = "Specified Money Claims";
    private static final String SERVICE_TYPE_DIVORCE = "Divorce";
    private static final String SERVICE_TYPE_FINREM = "Financial Remedy";
    private static final String SERVICE_TYPE_IAC = "Immigration and Asylum Appeals";
    private static final String SERVICE_TYPE_PRL = "Family Private Law";
    private static final String KERV = "kerv";

    private final String callbackUrl;
    private final String url;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public PciPalPaymentService(@Value("${pci-pal.api.url}") String url,
                                @Value("${pci-pal.callback-url}") String callbackUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.url = url;
        this.callbackUrl = callbackUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public TelephonyProviderAuthorisationResponse getTelephonyProviderLink(PciPalPaymentRequest pciPalPaymentRequest, TelephonyProviderAuthorisationResponse telephonyProviderAuthorisationResponse, String serviceType, String returnURL, TelephonySystem telephonySystem) {
        return withIOExceptionHandling(() -> {

            try {
                LOG.info("PciPalPaymentRequest: {}", objectMapper.writeValueAsString(pciPalPaymentRequest));
            } catch (IOException e) {
                LOG.error("Error serializing PciPalPaymentRequest to JSON", e);
            }

            try {
                LOG.info("TelephonyProviderAuthorisationResponse: {}", objectMapper.writeValueAsString(telephonyProviderAuthorisationResponse));
            } catch (IOException e) {
                LOG.error("Error serializing TelephonyProviderAuthorisationResponse to JSON", e);
            }

            try {
                LOG.info("Service Type: {}", serviceType);
            } catch (Exception e) {
                LOG.error("Error logging service type", e);
            }

            try {
                LOG.info("JSON telephonySystem: {}", objectMapper.writeValueAsString(telephonySystem));
            } catch (Exception e) {
                LOG.error("Error logging telephonySystem", e);
            }

            String flowId = telephonySystem.getFlowId(serviceType);
            LOG.info("Found flow id {} for service type {} using system {}", flowId, serviceType, telephonySystem.getSystemName());

            HttpPost httpPost = new HttpPost(telephonySystem.getLaunchURL());
            httpPost.addHeader(CONTENT_TYPE, APPLICATION_JSON.toString());
            httpPost.addHeader(authorizationHeader(telephonyProviderAuthorisationResponse.getAccessToken()));
            TelephonyProviderLinkIdRequest telephonyProviderLinkIdRequest = TelephonyProviderLinkIdRequest.telephonyProviderLinkIdRequestWith().flowId(flowId)
                .initialValues(TelephonyProviderLinkIdRequest.InitialValues.initialValuesWith()
                    .amount(new BigDecimal(pciPalPaymentRequest.getOrderAmount()).movePointRight(2).toString())
                    .callbackURL(callbackUrl)
                    .returnURL(returnURL)
                    .orderId(pciPalPaymentRequest.getOrderReference())
                    .currencyCode("GBP")
                    .build())
                .build();

            StringEntity entity = new StringEntity(objectMapper.writeValueAsString(telephonyProviderLinkIdRequest));
            String viewIdURL = telephonySystem.getViewIdURL();
            String launchURL = telephonySystem.getLaunchURL();

            httpPost.setEntity(entity);
            ClassicHttpResponse response = (ClassicHttpResponse) httpClient.execute(httpPost);
            if (response.getCode() == HttpStatus.SC_OK) {
                LOG.info("Success Response from PCI PAL!!!");
                TelephonyProviderLinkIdResponse telephonyProviderLinkIdResponse = objectMapper.readValue(response.getEntity().getContent(), TelephonyProviderLinkIdResponse.class);
                telephonyProviderAuthorisationResponse.setNextUrl(viewIdURL + telephonyProviderLinkIdResponse.getId() + "/framed");

            } else if (response.getCode() == HttpStatus.SC_BAD_REQUEST) {
                String responseBody = response.getEntity() != null ? new String(response.getEntity().getContent().readAllBytes()) : "No response body";
                LOG.info("ResponseCode: {} ResponseBody: {} flowId: {}   serviceType: {}    launchURL: {}   viewIdURL: {}   callbackUrl: {}   returnURL: {}  telephonyProvider: {}",
                    response.getCode(), responseBody, flowId, serviceType, launchURL,
                    viewIdURL, callbackUrl, returnURL, telephonySystem.getSystemName());
                throw new PciPalConfigurationException("This telephony system does not support telephony calls for the service '"+ serviceType +"'.");
            } else {
                String responseBody = response.getEntity() != null ? new String(response.getEntity().getContent().readAllBytes()) : "No response body";
                LOG.info("ResponseCode: {} ResponseBody: {} flowId: {}   serviceType: {}    launchURL: {}   viewIdURL: {}   callbackUrl: {}   returnURL: {}  telephonyProvider: {}",
                    response.getCode(), responseBody, flowId, serviceType, launchURL,
                    viewIdURL, callbackUrl, returnURL, telephonySystem.getSystemName());
                throw new PaymentException("Received error from PCI PAL!!!");
            }
            return telephonyProviderAuthorisationResponse;
        });
    }

    public TelephonyProviderAuthorisationResponse getPaymentProviderAuthorisationTokens(TelephonySystem telephonySystem, String userName) {
        return withIOExceptionHandling(() -> {
            LOG.info("tokensURL: {} with tenant name{}", telephonySystem.getTokensURL(), telephonySystem.getTenantName());
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", telephonySystem.getGrantType()));
            params.add(new BasicNameValuePair("tenantname", telephonySystem.getTenantName()));
            params.add(new BasicNameValuePair("username", userName));
            params.add(new BasicNameValuePair("client_id", telephonySystem.getClientId()));
            params.add(new BasicNameValuePair("client_secret", telephonySystem.getClientSecret()));

            HttpPost httpPost = new HttpPost(telephonySystem.getTokensURL());
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            ClassicHttpResponse response1 = (ClassicHttpResponse) httpClient.execute(httpPost);
            LOG.info("After 1st PCI PAL call!!!");
            return objectMapper.readValue(response1.getEntity().getContent(), TelephonyProviderAuthorisationResponse.class);
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
    public PciPalPayment create(CreatePaymentRequest createPaymentRequest, String serviceName) {
        return null;
    }

    @Override
    public void cancel(Payment payment, String ccdCaseNumber) {
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
    public PciPalPayment retrieve(PaymentFeeLink paymentFeeLink, String s) {
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

    @Override
    public void cancel(String paymentReference) {
    }

    @Override
    public void cancel(String cancelUrl, String serviceName) {

    }

    @Override
    public void cancel(Payment payment, String ccdCaseNumber, String serviceName) {
    }

    @Override
    public List<Payment> searchByCriteria(PaymentSearchCriteria searchCriteria) {
        return null;
    }

    private interface CheckedExceptionProvider<T> {
        T get() throws IOException, URISyntaxException;
    }

}
