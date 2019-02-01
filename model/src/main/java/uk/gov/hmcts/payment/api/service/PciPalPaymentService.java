package uk.gov.hmcts.payment.api.service;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.exceptions.PciPalClientException;
import uk.gov.hmcts.payment.api.external.client.dto.State;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PciPalPaymentService implements DelegatingPaymentService<PciPalPayment, String>  {
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
    private final String url;
    private final String apiKey;
    private final HttpClient httpClient;

    @Autowired
    public PciPalPaymentService(@Value("${pci-pal.api.url}") String url,@Value("${pci-pal.api.key}") String apiKey,
                                @Qualifier("paymentsHttpClient") HttpClient httpClient) {
        this.url = url;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }


    public String sendIntialPaymentRequest(PciPalPaymentRequest pciPalPaymentRequest,String serviceType) {
        return withIOExceptionHandling(() -> {
        String ppAccountID = null;
        if (serviceType.equalsIgnoreCase(serviceType))
            ppAccountID = ppAccountIDDivorce;
        else if(serviceType.equalsIgnoreCase(SERVICE_TYPE_CMC))
            ppAccountID = ppAccountIDCmc;
        else if(serviceType.equalsIgnoreCase(SERVICE_TYPE_PROBATE))
            ppAccountID = ppAccountIDProbate;

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("apiKey", apiKey));
        params.add(new BasicNameValuePair("ppAccountId", ppAccountID));
        params.add(new BasicNameValuePair("renderMethod", "HTML"));
        params.add(new BasicNameValuePair("amount", pciPalPaymentRequest.getOrderAmount()));
        params.add(new BasicNameValuePair("orderCurrency", pciPalPaymentRequest.getOrderCurrency()));
        params.add(new BasicNameValuePair("orderReference", pciPalPaymentRequest.getOrderReference()));
        params.add(new BasicNameValuePair("callbackURL", pciPalPaymentRequest.getCallbackURL()));
        HttpPost request = postRequestFor( url, new UrlEncodedFormEntity(params));
        HttpResponse response = httpClient.execute(request);
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
             return responseString;
        });
        }

    private HttpPost postRequestFor(String url, UrlEncodedFormEntity entity)  {
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        return request;
    }

    private <T> T withIOExceptionHandling(CheckedExceptionProvider<T> provider) {
        try {
            return provider.get();
        } catch (IOException e) {
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
        T get() throws IOException;
    }


}
