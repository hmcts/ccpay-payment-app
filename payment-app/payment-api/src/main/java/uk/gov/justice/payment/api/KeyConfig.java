package uk.gov.justice.payment.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
@ConfigurationProperties(prefix="gov.pay.auth")
public class KeyConfig {
    private Map<String,String> key;

    public Map<String, String> getKey() {
        return key;
    }

    public void setKey(Map<String, String> key) {
        this.key = key;
    }



}