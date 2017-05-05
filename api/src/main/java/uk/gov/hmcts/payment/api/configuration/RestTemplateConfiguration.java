package uk.gov.hmcts.payment.api.configuration;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.logging.OutboundRequestLoggingInterceptor;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.http.HttpHost.create;

@Configuration
public class RestTemplateConfiguration {
    private final String httpsProxy;

    @Autowired
    public RestTemplateConfiguration(@Value("${proxy}") String httpsProxy) {
        this.httpsProxy = httpsProxy;
    }

    @Bean
    public RestTemplate restTemplate(CloseableHttpClient paymentsHttpClient) {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(paymentsHttpClient));
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    @Bean(name = {"paymentsHttpClient", "serviceTokenParserHttpClient", "userTokenParserHttpClient"})
    public CloseableHttpClient paymentsHttpClient() {
        HttpClientBuilder clientBuilder = HttpClients.custom()
            .addInterceptorFirst((HttpRequestInterceptor) new OutboundRequestLoggingInterceptor())
            .addInterceptorLast((HttpResponseInterceptor) new OutboundRequestLoggingInterceptor());

        if (!isNullOrEmpty(httpsProxy)) {
            clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(create(httpsProxy)));
        }

        return clientBuilder.build();
    }
}
