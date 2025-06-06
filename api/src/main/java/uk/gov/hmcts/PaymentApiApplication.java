package uk.gov.hmcts;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import uk.gov.hmcts.payment.api.logging.Markers;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;

import jakarta.servlet.ServletContextListener;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@EnableCaching
@EnableFeignClients
@EnableAsync
@SpringBootApplication
@OpenAPIDefinition
public class PaymentApiApplication {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentApiApplication.class);

    @Value("${pci-pal.antenna.prl.flow.id}")
    private static String prlFlowId;

    public static void main(String[] args) {
        try {
            SpringApplication.run(PaymentApiApplication.class, args);
        } catch (RuntimeException ex) {
            LOG.error(Markers.fatal, "Application crashed with error message: ", ex);
        }
    }

    @Bean
    ServletListenerRegistrationBean<ServletContextListener> myServletListener() {
        ServletListenerRegistrationBean<ServletContextListener> srb = new ServletListenerRegistrationBean<>();
        srb.setListener(new PaymentServletContextListener());
        return srb;
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new CaffeineCache("feesDtoMap", Caffeine.newBuilder()
                .expireAfterWrite(1440, TimeUnit.MINUTES)
                .build()),
            new CaffeineCache("sites", Caffeine.newBuilder()
                .expireAfterWrite(48, TimeUnit.HOURS)
                .build())
        ));
        return cacheManager;
    }

    @Bean
    public PluginRegistry<LinkDiscoverer, MediaType> discoverers(
        OrderAwarePluginRegistry<LinkDiscoverer, MediaType> relProviderPluginRegistry) {
        return relProviderPluginRegistry;
    }
}