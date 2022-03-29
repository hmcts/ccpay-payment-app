package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.ff4j.FF4j;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class ServiceRequestDomainServiceTest  {

    @MockBean
    private TopicClientService topicClientService;

    @Autowired
    ServiceRequestDomainService serviceRequestDomainService;

    @MockBean
    private TopicClientProxy topicClientProxy;

    @Autowired
    private FF4j ff4j;

    //@Test
    public void deadLetterTest() throws ServiceBusException, InterruptedException, IOException {

        IMessage msg = mock(IMessage.class);
        IMessageReceiver subscriptionClient = mock(IMessageReceiver.class);
        TopicClientProxy topicClientProxy = mock(TopicClientProxy.class);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> data = new HashMap<>();

        data.put("action", "action");
        data.put("case_id", "caseId");
        data.put("order_reference", "orderReference");
        data.put("responsible_party", "responsibleParty");

        byte[] dataInBytes = mapper.writeValueAsBytes(data);
        Map <String ,String>  msgProperties = new HashMap<>();
        msgProperties.put("action", "500");

        when(subscriptionClient.receive()).thenReturn(msg,null);
        when(msg.getBody()).thenReturn(dataInBytes);
        when(msg.getProperties()).thenReturn(msgProperties);
        when(topicClientService.getTopicClientProxy()).thenReturn(topicClientProxy);
        doNothing().when(topicClientProxy).send(any(IMessage.class));
        doNothing().when(topicClientProxy).close();
        serviceRequestDomainService.deadLetterProcess(subscriptionClient);
        verify(topicClientProxy, times(1)).close();
    }

}
