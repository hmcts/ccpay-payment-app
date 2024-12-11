package uk.gov.hmcts.payment.api.configuration;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Value("${iac.connect.timeout:5000}")
    private int iacConnectTimeout;

    @Value("${iac.read.timeout:5000}")
    private int iacReadTimeout;

    @Value("${liberata.connect.timeout:10000}")
    private int liberataConnectTimeout;

    @Value("${liberata.read.timeout:10000}")
    private int liberataReadTimeout;

    @Bean(name = {"serviceTokenParserHttpClient", "userTokenParserHttpClient"})
    public CloseableHttpClient paymentsHttpClient() {
        return HttpClients.custom()
            .useSystemProperties()
            .build();
    }

    @Bean (value = "restTemplatePaymentGroup")
    public RestTemplate restTemplatePaymentGroup() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean (value = "restTemplateLiberata")
    public RestTemplate restTemplateLiberata() {
        return createRestTemplate(liberataReadTimeout, liberataConnectTimeout);
    }

    @Bean (value = "restTemplateRefundsGroup")
    public RestTemplate restTemplateRefundsGroup() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean (value = "restTemplateIacSupplementaryInfo")
    public RestTemplate restTemplateIacSupplementaryInfo() {
        return createRestTemplate(iacReadTimeout, iacConnectTimeout);
    }

    @Bean("restTemplateIdam")
    public RestTemplate restTemplateIdam() {
        return  new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean("restTemplateRefData")
    public RestTemplate restTemplateRefData() {
        return  new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean("restTemplateRefundCancel")
    public RestTemplate restTemplateRefundCancel() {
        return  new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean("restTemplateGetRefund")
    public RestTemplate restTemplateGetRefund() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    private RestTemplate createRestTemplate(int readTimeout, int connectTimeout) {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(readTimeout))
                .build())
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setSocketTimeout(Timeout.ofMilliseconds(readTimeout))
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                .build())
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}
