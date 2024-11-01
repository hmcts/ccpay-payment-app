package uk.gov.hmcts.payment.api.configuration;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Value("${iac.connect.timeout}")
    private String iacConnectTimeout;

    @Value("${iac.read.timeout}")
    private String iacReadTimeout;

    @Value("${liberata.connect.timeout:10000}")
    private String liberataConnectTimeout;

    @Value("${liberata.read.timeout:10000}")
    private String liberataReadTimeout;

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
        int connectTimeout = Integer.parseInt(liberataConnectTimeout);
        int readTimeout = Integer.parseInt(liberataReadTimeout);

        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
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

    @Bean (value = "restTemplateRefundsGroup")
    public RestTemplate restTemplateRefundsGroup() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean (value = "restTemplateIacSupplementaryInfo")
    public RestTemplate restTemplateIacSupplementaryInfo() {
        int connectTimeout = Integer.parseInt(iacConnectTimeout);
        int readTimeout = Integer.parseInt(iacReadTimeout);

        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
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
        return  new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean ("restTemplateAccount")
    public RestTemplate restTemplateAccount() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }
}
