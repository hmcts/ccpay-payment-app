package uk.gov.hmcts.payment.api.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.TelephonyProviderAuthorisationResponse;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PciPalPaymentServiceTest {

    @Mock
    private HttpClient httpClient;

    private PciPalPaymentService pciPalPaymentService;

    private ObjectMapper objectMapper;

    public static final String MOCKJSONLINKRESPONSE = """
    {
        "Id": "mockId",
        "LinkId": "mockLinkId"
    }""";

    public static final String MOCKJSONRESPONSE = """
    {
        "access_token": "mockAccessToken",
        "token_type": "Bearer",
        "expires_in": "3600",
        "refresh_token": "mockRefreshToken",
        "client_id": "mockClientId",
        "tenantName": "mockTenantName",
        ".issued": "2023-01-01T00:00:00Z",
        ".expires": "2023-01-01T01:00:00Z",
        "next_url": "http://mockurl.com"
    }
    """;

    @BeforeEach
    void setUp() throws Exception {
        // Mock the @Value fields
        objectMapper = new ObjectMapper();
        pciPalPaymentService = new PciPalPaymentService(
            "http://mock-api-url", // Mocked API URL
            "http://mock-callback-url", // Mocked callback URL
            httpClient,
            objectMapper
        );
        setPrivateField(pciPalPaymentService, "tokensURL", "http://mock-tokens-url");
        setPrivateField(pciPalPaymentService, "tenantName", "mockTenantName");
        setPrivateField(pciPalPaymentService, "grantType", "mockGrantType");
        setPrivateField(pciPalPaymentService, "userName", "mockUserName");
        setPrivateField(pciPalPaymentService, "clientId", "mockClientId");
        setPrivateField(pciPalPaymentService, "clientSecret", "mockClientSecret");
        setPrivateField(pciPalPaymentService, "viewKervIdURL", "mockViewKervIdURL");
        setPrivateField(pciPalPaymentService, "viewIdURL", "mockViewIdURL");
        setPrivateField(pciPalPaymentService, "divorceKervFlowId", "divorceKervFlowId");
        setPrivateField(pciPalPaymentService, "launchKervURL", "mockLaunchKervURL");
        setPrivateField(pciPalPaymentService, "launchURL", "mockLaunchURL");
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void shouldReturnTelephonyProviderAuthorisationResponse() throws Exception {
        // Arrange
        InputStream mockInputStream = new ByteArrayInputStream(MOCKJSONRESPONSE.getBytes(StandardCharsets.UTF_8));
        HttpEntity mockEntity = mock(HttpEntity.class);
        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);

        // Define the behavior of the mocked response

        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);

        // Act
        TelephonyProviderAuthorisationResponse result = pciPalPaymentService.getPaymentProviderAutorisationTokens();

        // Assert
        assertNotNull(result);
        assertEquals("mockAccessToken", result.getAccessToken());
    }

    @Test
    void shouldReturnTelephonyProviderAuthorisationResponseWithKerv() throws Exception {
        // Arrange
        String idamUserName = "mockUserName";
        InputStream mockInputStream = new ByteArrayInputStream(MOCKJSONRESPONSE.getBytes(StandardCharsets.UTF_8));
        HttpEntity mockEntity = mock(HttpEntity.class);
        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);

        // Define the behavior of the mocked response
        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);

        // Act
        TelephonyProviderAuthorisationResponse result = pciPalPaymentService.getKervPaymentProviderAutorisationTokens(idamUserName);

        // Assert
        assertNotNull(result);
        assertEquals("mockAccessToken", result.getAccessToken());
        assertEquals("mockUserName", getPrivateField(pciPalPaymentService, "userName"));
    }

    @Test
    void shouldReturnCorrectFlowIdForServiceTypeAndTelephonyProvider() throws Exception {
        // Arrange
        String serviceType = "Divorce";
        String telephonyProvider = "kerv";
        setPrivateField(pciPalPaymentService, "divorceKervFlowId", "mockDivorceKervFlowId");

        // Act
        String flowId = pciPalPaymentService.getFlowId(serviceType, telephonyProvider);

        // Assert
        assertNotNull(flowId);
        assertEquals("mockDivorceKervFlowId", flowId);
    }

    @Test
    void shouldThrowExceptionForUnsupportedServiceType() {
        // Arrange
        String serviceType = "UnsupportedService";
        String telephonyProvider = "kerv";

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class,
            () -> pciPalPaymentService.getFlowId(serviceType, telephonyProvider));
        assertEquals("This telephony system does not support telephony calls for the service 'UnsupportedService'.", exception.getMessage());
    }

    @Test
    void shouldReturnAuthorizationHeader() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        String authorizationKey = "mockAuthorizationKey";

        // Access the private method
        Method method = PciPalPaymentService.class.getDeclaredMethod("authorizationHeader", String.class);
        method.setAccessible(true); // Make the private method accessible

        // Act
        Header header = (Header) method.invoke(pciPalPaymentService, authorizationKey);

        // Assert
        assertNotNull(header);
        assertEquals("Bearer mockAuthorizationKey", header.getValue());
        assertEquals(HttpHeaders.AUTHORIZATION, header.getName());
    }

    @Test
    void shouldCreatePciPalPayment() {
        // Arrange
        PaymentServiceRequest paymentServiceRequest = mock(PaymentServiceRequest.class);

        // Act
        PciPalPayment result = pciPalPaymentService.create(paymentServiceRequest);

        // Assert
        assertNotNull(result);
        assertEquals("spoof_id", result.getPaymentId());
        assertNotNull(result.getState());
        assertEquals("code", result.getState().getCode());
        assertFalse(result.getState().getFinished());
        assertEquals("message", result.getState().getMessage());
        assertEquals("created", result.getState().getStatus());
    }

    @Test
    void shouldReturnTelephonyProviderLinkSuccessfully() throws IOException {
        // Arrange
        PciPalPaymentRequest mockpciPalPaymentRequest = mock(PciPalPaymentRequest.class);
        String serviceType = "Divorce";
        String telephonyProvider = "kerv";
        TelephonyProviderAuthorisationResponse mockTelephonyProviderAuthorisationResponse = mock(TelephonyProviderAuthorisationResponse.class);

        InputStream mockInputStream = new ByteArrayInputStream(MOCKJSONLINKRESPONSE.getBytes(StandardCharsets.UTF_8));
        HttpEntity mockEntity = mock(HttpEntity.class);
        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);

        // Act
        when(mockTelephonyProviderAuthorisationResponse.getAccessToken()).thenReturn(createTelephonyProviderAuthorisationResponse().getAccessToken());
        when(mockTelephonyProviderAuthorisationResponse.getNextUrl()).thenReturn("http://mockurl.com");
        when(mockpciPalPaymentRequest.getOrderAmount()).thenReturn("100.00");
        when(mockpciPalPaymentRequest.getOrderReference()).thenReturn("mockOrderReference");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);
        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);

        // Call the method under test
        TelephonyProviderAuthorisationResponse methodUnderTest = pciPalPaymentService.getTelephonyProviderLink(
        mockpciPalPaymentRequest,mockTelephonyProviderAuthorisationResponse,
        serviceType,
        "http://mock-return-url",
        telephonyProvider);

        // Assert
        assertNotNull(methodUnderTest);
        assertNotNull(methodUnderTest.getAccessToken());
        assertNotNull(methodUnderTest.getNextUrl());
    }


    @Test
    void shouldThrowExceptionForBadRequestResponse() throws IOException {
        // Arrange
        PciPalPaymentRequest mockRequest = mock(PciPalPaymentRequest.class);
        TelephonyProviderAuthorisationResponse mockAuthResponse = mock(TelephonyProviderAuthorisationResponse.class);
        when(mockAuthResponse.getAccessToken()).thenReturn("mockAccessToken");
        when(mockRequest.getOrderAmount()).thenReturn("100.00");
        when(mockRequest.getOrderReference()).thenReturn("mockOrderReference");

        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream("Bad Request".getBytes(StandardCharsets.UTF_8)));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            pciPalPaymentService.getTelephonyProviderLink(mockRequest, mockAuthResponse, "Divorce", "http://mock-return-url", "kerv");
        });
        assertEquals("This telephony system does not support telephony calls for the service 'Divorce'.", exception.getMessage());
    }


    @Test
    void shouldThrowExceptionForInternalServerErrorResponse() throws IOException {
        // Arrange
        PciPalPaymentRequest mockRequest = mock(PciPalPaymentRequest.class);
        TelephonyProviderAuthorisationResponse mockAuthResponse = mock(TelephonyProviderAuthorisationResponse.class);
        when(mockAuthResponse.getAccessToken()).thenReturn("mockAccessToken");
        when(mockRequest.getOrderAmount()).thenReturn("100.00");
        when(mockRequest.getOrderReference()).thenReturn("mockOrderReference");

        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream("Internal Server Error".getBytes(StandardCharsets.UTF_8)));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            pciPalPaymentService.getTelephonyProviderLink(mockRequest, mockAuthResponse, "Divorce", "http://mock-return-url", "kerv");
        });
        assertEquals("Received error from PCI PAL!!!", exception.getMessage());
    }


    private TelephonyProviderAuthorisationResponse createTelephonyProviderAuthorisationResponse() {
        TelephonyProviderAuthorisationResponse response = new TelephonyProviderAuthorisationResponse();
        response.setAccessToken("mockAccessToken");
        response.setTokenType("Bearer");
        response.setExpiresIn("3600");
        response.setRefreshToken("mockRefreshToken");
        response.setClientId("mockClientId");
        response.setTenantName("mockTenantName");
        response.setIssued("2023-01-01T00:00:00Z");
        response.setExpires("2023-01-01T01:00:00Z");
        response.setNextUrl("http://mockurl.com");

        return response;
    }
}
