package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Test;
import org.mockito.Spy;

import static org.mockito.Mockito.*;

public class TopicClientProxyTest {

    @Spy
    private TopicClient client = mock(TopicClient.class);

    @Spy
    private IMessage message = mock(IMessage.class);

    @Spy
    TopicClientProxy proxy = new TopicClientProxy("conn", "topic");

    @Test
    public void shouldSendMessageSuccessfullyOnFirstAttempt() throws Exception {
        var method = TopicClientProxy.class.getDeclaredMethod("send", TopicClient.class, IMessage.class);
        method.setAccessible(true);

        method.invoke(proxy, client, message);

        verify(client, times(1)).send(message);
    }

    @Test
    public void shouldRetryAndSucceedOnSecondAttempt() throws Exception {
        doThrow(new ServiceBusException(false, "fail"))
            .doNothing()
            .when(client).send(message);

        var method = TopicClientProxy.class.getDeclaredMethod("send", TopicClient.class, IMessage.class);
        method.setAccessible(true);

        method.invoke(proxy, client, message);

        verify(client, times(2)).send(message);
    }

    @Test
    public void shouldThrowAfterMaxRetries() throws Exception {
        doThrow(new ServiceBusException(false, "fail"))
            .when(client).send(message);

        var method = TopicClientProxy.class.getDeclaredMethod("send", TopicClient.class, IMessage.class);
        method.setAccessible(true);

        try {
            method.invoke(proxy, client, message);
        } catch (Exception e) {
            // Expected: InvocationTargetException wrapping ServiceBusException
            assert e.getCause() instanceof ServiceBusException;
        }

        verify(client, times(3)).send(message);
    }
}
