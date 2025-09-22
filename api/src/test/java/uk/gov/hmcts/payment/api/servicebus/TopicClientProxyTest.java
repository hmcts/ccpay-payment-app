package uk.gov.hmcts.payment.api.servicebus;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TopicClientProxyTest {

    @Spy
    private TopicClient client = mock(TopicClient.class);

    @Spy
    private IMessage message = mock(IMessage.class);

    @Test
    public void shouldSendAMessageTest() throws Exception {
        when(client.sendAsync(any(IMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        client.sendAsync(message).join();

        verify(client, times(1)).sendAsync(message);
    }

    @Test
    public void shouldSendMessageSuccessfullyOnFirstAttempt() throws Exception {
        TopicClientProxy proxy = Mockito.spy(new TopicClientProxy("conn", "topic"));

        var method = TopicClientProxy.class.getDeclaredMethod("send", TopicClient.class, IMessage.class);
        method.setAccessible(true);

        method.invoke(proxy, client, message);

        verify(client, times(1)).send(message);
    }

    @Test
    public void shouldRetryAndSucceedOnSecondAttempt() throws Exception {
        TopicClientProxy proxy = Mockito.spy(new TopicClientProxy("conn", "topic"));

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
        TopicClientProxy proxy = Mockito.spy(new TopicClientProxy("conn", "topic"));

        doThrow(new ServiceBusException(false, "fail"))
            .when(client).send(message);

        var method = TopicClientProxy.class.getDeclaredMethod("send", TopicClient.class, IMessage.class);
        method.setAccessible(true);

        try {
            method.invoke(proxy, client, message);
        } catch (Exception e) {
            assert e.getCause() instanceof ServiceBusException;
        }

        verify(client, times(3)).send(message);
    }

    @Test
    public void testSendWithKeepClientAliveFalse() throws Exception {
        TopicClientProxy proxy = Mockito.spy(new TopicClientProxy("Endpoint=sb://servicebus-test.servicebus.windows.net/;SharedAccessKey=test", "topic"));

        MessageBody body = MessageBody.fromValueData("Test message body".getBytes());
        when(message.getMessageId()).thenReturn(UUID.randomUUID().toString());
        when(message.getBody()).thenReturn(body.toString().getBytes());
        when(message.getProperties()).thenReturn(Map.of("key", "value"));
        when(message.getScheduledEnqueueTimeUtc()).thenReturn(Instant.now().plus(Duration.ofMinutes(5)));

        when(client.sendAsync(any(IMessage.class))).thenReturn(CompletableFuture.completedFuture(null));
        doReturn(client).when(proxy).newTopicClient();

        proxy.setKeepClientAlive(false);
        proxy.send(message);

        verify(client, times(1)).send(any(IMessage.class));
        verify(client).close();
    }

    @Test
    public void testSendWithKeepClientAliveTrue() throws Exception {
        TopicClientProxy proxy = Mockito.spy(new TopicClientProxy("Endpoint=sb://servicebus-test.servicebus.windows.net/;SharedAccessKey=test", "topic"));

        MessageBody body = MessageBody.fromValueData("Test message body".getBytes());
        when(message.getMessageId()).thenReturn(UUID.randomUUID().toString());
        when(message.getBody()).thenReturn(body.toString().getBytes());
        when(message.getProperties()).thenReturn(Map.of("key", "value"));
        when(message.getScheduledEnqueueTimeUtc()).thenReturn(Instant.now().plus(Duration.ofMinutes(5)));

        when(client.sendAsync(any(IMessage.class))).thenReturn(CompletableFuture.completedFuture(null));
        doReturn(client).when(proxy).newTopicClient();

        proxy.setKeepClientAlive(true);
        proxy.send(message);

        verify(client, times(1)).send(any(IMessage.class));
        verify(client, never()).close();
    }
}
