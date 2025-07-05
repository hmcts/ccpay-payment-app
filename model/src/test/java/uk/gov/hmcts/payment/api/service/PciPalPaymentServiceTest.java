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
import uk.gov.hmcts.payment.api.v1.model.exceptions.PciPalConfigurationException;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class PciPalPaymentServiceTest {

    @Mock
    private HttpClient httpClient;

    private PciPalPaymentService pciPalPaymentService;

    private ObjectMapper objectMapper;

    private PciPalPaymentRequest pciPalPaymentRequest = PciPalPaymentRequest.pciPalPaymentRequestWith()
            .orderAmount("100.00")
            .orderReference("mockOrderReference")
            .customData2("mockCustomData2")
            .orderCurrency("GBP")
            .build();

    private static final TelephonySystem telephonySystem = KervTelephonySystem.builder()
        .probateFlowId("mockProbateKervFlowId")
        .divorceFlowId("mockDivorceKervFlowId")
        .strategicFlowId("mockStrategicKervFlowId")
        .iacFlowId("mockIacKervFlowId")
        .prlFlowId("mockPrlKervFlowId")
        .kervViewIdURL("mockKervViewIdURL")
        .kervLaunchURL("mockKervLaunchURL")
        .kervTokensURL("mockKervTokensURL")
        .kervGrantType("mockKervGrantType")
        .kervTenantName("mockKervTenantName")
        .kervClientId("mockKervClientId")
        .kervClientSecret("mockKervClientSecret")
        .build();

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
    }

    @Test
    void shouldReturnTelephonyProviderAuthorizationTokens() throws Exception {
        // Arrange
        String userName = "mockUserName";
        String mockResponseJson = """
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

        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(mockResponseJson.getBytes(StandardCharsets.UTF_8)));
        TelephonyProviderAuthorisationResponse expectedResponse = new TelephonyProviderAuthorisationResponse();
        expectedResponse.setAccessToken("mockAccessToken");

        // Act
        TelephonyProviderAuthorisationResponse result = pciPalPaymentService.getPaymentProviderAuthorisationTokens(telephonySystem, userName);

        // Assert
        assertNotNull(result);
        assertEquals("mockAccessToken", result.getAccessToken());
        verify(httpClient, times(1)).execute(any(HttpPost.class));
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
        TelephonyProviderAuthorisationResponse mockTelephonyProviderAuthorisationResponse = mock(TelephonyProviderAuthorisationResponse.class);
        when(mockTelephonyProviderAuthorisationResponse.getAccessToken()).thenReturn("mockAccessToken");
        when(mockTelephonyProviderAuthorisationResponse.getNextUrl()).thenReturn("http://mock-next-url");

        InputStream mockInputStream = new ByteArrayInputStream(MOCKJSONLINKRESPONSE.getBytes(StandardCharsets.UTF_8));
        HttpEntity mockEntity = mock(HttpEntity.class);
        ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);
        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);

        // Act
        TelephonyProviderAuthorisationResponse result = pciPalPaymentService.getTelephonyProviderLink(
            pciPalPaymentRequest,
            mockTelephonyProviderAuthorisationResponse,
            "Divorce",
            "http://mock-return-url",
            telephonySystem
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAccessToken());
        assertNotNull(result.getNextUrl());

        verify(mockTelephonyProviderAuthorisationResponse, times(1)).getNextUrl();
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
        PciPalConfigurationException exception = assertThrows(PciPalConfigurationException.class, () -> {
            pciPalPaymentService.getTelephonyProviderLink(mockRequest, mockAuthResponse, "Divorce", "http://mock-return-url", telephonySystem);
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
            pciPalPaymentService.getTelephonyProviderLink(mockRequest, mockAuthResponse, "Divorce", "http://mock-return-url", telephonySystem);
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
