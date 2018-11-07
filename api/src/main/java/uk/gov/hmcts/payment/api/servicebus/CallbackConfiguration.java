package uk.gov.hmcts.payment.api.servicebus;

import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CallbackConfiguration extends CamelAutoConfiguration {

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Bean
    public JmsConnectionFactory jmsConnectionFactory() {
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(
            environment.getProperty("servicebus.username"),
            environment.getProperty("servicebus.password"),
            environment.getProperty("servicebus.remoteUri"));
        jmsConnectionFactory.setReceiveLocalOnly(true);

        return jmsConnectionFactory;
    }

    @Bean
    public JmsConfiguration jmsConfiguration() {
        JmsConfiguration config = new JmsConfiguration();
        config.setConnectionFactory(jmsConnectionFactory());
        config.setCacheLevelName("CACHE_AUTO");
        return config;
    }

    @Bean
    public AMQPComponent amqp() {
        AMQPComponent comp = new AMQPComponent();
        comp.setConfiguration(jmsConfiguration());
        return comp;
    }

}
