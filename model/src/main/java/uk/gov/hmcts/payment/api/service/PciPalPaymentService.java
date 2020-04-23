package uk.gov.hmcts.payment.api.service;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
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
import uk.gov.hmcts.payment.api.external.client.dto.State;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


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

    @Autowired
    public PciPalPaymentService(@Value("${pci-pal.api.url}") String url,
                                @Value("${pci-pal.callback-url}") String callbackUrl) {
        this.url = url;
        this.callbackUrl = callbackUrl;
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

    @Override
    public void cancel(String paymentReference) {}

    private interface CheckedExceptionProvider<T> {
        T get() throws IOException, URISyntaxException;
    }

    @Override
    public void cancel(String paymentReference) {}

}
