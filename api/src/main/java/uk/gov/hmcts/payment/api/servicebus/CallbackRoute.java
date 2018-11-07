package uk.gov.hmcts.payment.api.servicebus;

import org.apache.camel.builder.RouteBuilder;

public class CallbackRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("ampq:topic:topic1/subscriptions1/my-subscription").to("file://sb.txt");
    }
}
