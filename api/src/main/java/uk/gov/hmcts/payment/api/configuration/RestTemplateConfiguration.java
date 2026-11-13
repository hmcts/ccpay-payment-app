package uk.gov.hmcts.payment.api.configuration;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

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

    @Primary
    @Bean
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


    @Bean(name = "liberataRestTemplate")
    public RestTemplate liberataRestTemplate() {

        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory();

        // Connection settings
        requestFactory.setConnectionRequestTimeout(30000);
        requestFactory.setConnectionRequestTimeout(30000);
        requestFactory.setReadTimeout(30000);

        RestTemplate restTemplate = new RestTemplate(
            new BufferingClientHttpRequestFactory(requestFactory));

        restTemplate.setInterceptors(
            Collections.singletonList(new LoggingInterceptor()));

        return restTemplate;
    }

    @Slf4j
    public static class LoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

            logRequest(request, body);

            ClientHttpResponse response = execution.execute(request, body);

            logResponse(response);

            return response;
        }

        private void logRequest(HttpRequest request, byte[] body) {

            System.out.println("=================================================");
            System.out.println("HTTP REQUEST");
            System.out.println("=================================================");
            System.out.println("URI         : " + request.getURI());
            System.out.println("Method      : " + request.getMethod());

            System.out.println("Headers:");
            request.getHeaders().forEach((key, value) ->
                System.out.println(key + " : " + value));

            System.out.println("Body:");
            System.out.println(new String(body, StandardCharsets.UTF_8));

            System.out.println("=================================================");
        }

        private void logResponse(ClientHttpResponse response)
            throws IOException {

            String responseBody =
                StreamUtils.copyToString(
                    response.getBody(),
                    StandardCharsets.UTF_8);

            System.out.println("=================================================");
            System.out.println("HTTP RESPONSE");
            System.out.println("=================================================");
            System.out.println("Status Code : " + response.getStatusCode());
            System.out.println("Status Text : " + response.getStatusText());

            System.out.println("Headers:");
            response.getHeaders().forEach((key, value) ->
                System.out.println(key + " : " + value));

            System.out.println("Body:");
            System.out.println(responseBody);

            System.out.println("=================================================");
        }
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
