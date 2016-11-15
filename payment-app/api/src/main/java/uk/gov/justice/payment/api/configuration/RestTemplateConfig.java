package uk.gov.justice.payment.api.configuration;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.logging.OutboundRequestLoggingInterceptor;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.http.HttpHost.create;

@Configuration
public class RestTemplateConfig {
    private final String httpsProxy;

    @Autowired
    public RestTemplateConfig(@Value("${https_proxy:}") String httpsProxy) {
        this.httpsProxy = httpsProxy;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(requestFactory()));
        restTemplate.getInterceptors().add(new OutboundRequestLoggingInterceptor());
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    private HttpComponentsClientHttpRequestFactory requestFactory() {
        HttpClientBuilder clientBuilder = HttpClients.custom();

        if (!isNullOrEmpty(httpsProxy)) {
            clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(create(httpsProxy)));
        }

        return new HttpComponentsClientHttpRequestFactory(clientBuilder.build());
    }
}
