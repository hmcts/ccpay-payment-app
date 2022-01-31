package uk.gov.hmcts.payment.api.componenttests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.IdamFullNameRetrivalResponse;
import uk.gov.hmcts.payment.api.dto.IdamUserIdDetailsResponse;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.exception.UserNotFoundException;
import uk.gov.hmcts.payment.api.service.IdamServiceImpl;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)

public class IdamServiceImplTest {

    @InjectMocks
    private IdamServiceImpl idamService;

    @Mock
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;

    @Test
   public void getResponseOnValidToken() {

       MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        IdamUserIdResponse mockIdamUserIdResponse = IdamUserIdResponse.idamUserIdResponseWith()
            .familyName("VP")
            .givenName("VP")
            .name("VP")
            .sub("V_P@gmail.com")
            .roles(Arrays.asList("vp"))
            .uid("986-erfg-kjhg-123")
            .build();

        ResponseEntity<IdamUserIdResponse> responseEntity = new ResponseEntity<>(mockIdamUserIdResponse, HttpStatus.OK);

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                                       eq(IdamUserIdResponse.class)
        )).thenReturn(responseEntity);

        IdamUserIdResponse idamUserIdResponse = idamService.getUserId(header);
        assertEquals(mockIdamUserIdResponse.getUid(), idamUserIdResponse.getUid());
    }

    @Test
    public void getExceptionOnInvalidToken() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "user not found"));

        assertThrows(UserNotFoundException.class, () -> {
            idamService.getUserId(header);
        });
    }

    @Test
   public void getExceptionOnTokenReturnsNullResponse() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        ResponseEntity<IdamUserIdResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdResponse.class)
        )).thenReturn(responseEntity);
        assertThrows(UserNotFoundException.class, () -> {
            idamService.getUserId(header);
        });
    }

    @Test
   public void getExceptionOnValidToken() throws Exception {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdResponse.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT, "Gateway timeout"));
        assertThrows(GatewayTimeoutException.class, () -> {
            idamService.getUserId(header);
        });
    }

    @Test
   public  void validateResponseDto() {
        IdamUserIdResponse idamUserIdResponse = IdamUserIdResponse.idamUserIdResponseWith()
            .familyName("VP")
            .givenName("VP")
            .name("VP")
            .sub("V_P@gmail.com")
            .roles(Arrays.asList("vp"))
            .uid("986-erfg-kjhg-123")
            .build();

        assertEquals("VP", idamUserIdResponse.getFamilyName());
        assertEquals("VP", idamUserIdResponse.getGivenName());
        assertEquals("VP", idamUserIdResponse.getName());
        assertEquals(Arrays.asList("vp"), idamUserIdResponse.getRoles());
        assertEquals("986-erfg-kjhg-123", idamUserIdResponse.getUid());
        assertEquals("V_P@gmail.com", idamUserIdResponse.getSub());
    }

    @Test
    public void givenNoIdamResponse_whenGetUserIdentityData_thenUserNotFoundException() {
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        ResponseEntity<IdamFullNameRetrivalResponse[]> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamFullNameRetrivalResponse[].class)
        )).thenReturn(responseEntity);

        Exception exception = Assertions.assertThrows(UserNotFoundException.class, () ->
            idamService.getUserIdentityData(header, "AA"));
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Internal Server error. Please, try again later"));
    }

    @Test
    public void givenIdamResponse_whenGetUserIdentityData_thenDistinctUserIdSetIsReceived() {
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        IdamFullNameRetrivalResponse user = IdamFullNameRetrivalResponse
            .idamFullNameRetrivalResponseWith()
            .id("AA")
            .email("aa@gmail.com")
            .forename("AAA")
            .surname("BBB")
            .roles(List.of("caseworker-refund", "caseworker-damage"))
            .active(true)
            .lastModified("2021-02-20T11:03:08.067Z")
            .build();

        final IdamFullNameRetrivalResponse[] responses = {user};
        ResponseEntity<IdamFullNameRetrivalResponse[]> responseEntity = new ResponseEntity<>(responses, HttpStatus.OK);
        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamFullNameRetrivalResponse[].class)
        )).thenReturn(responseEntity);

        UserIdentityDataDto resultDto = idamService.getUserIdentityData(header, "AA");

        assertEquals("AAA BBB", resultDto.getFullName());
        assertEquals("aa@gmail.com", resultDto.getEmailId());
    }

    @Test
    public void getUserDetailsTest() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        IdamUserIdDetailsResponse mockIdamUserIdDetailsResponse = IdamUserIdDetailsResponse.
            idamUserIdResponseWith()
            .forename("AAA")
            .surname("VP")
            .email("V_P@gmail.com")
            .roles(Arrays.asList("vp"))
            .id("986-erfg-kjhg-123")
            .build();

        ResponseEntity<IdamUserIdDetailsResponse> responseEntity = new ResponseEntity<>
            (mockIdamUserIdDetailsResponse, HttpStatus.OK);

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdDetailsResponse.class)
        )).thenReturn(responseEntity);

        String idamUserIdDetailsResponse = idamService.getUserDetails(header);
        assertEquals(mockIdamUserIdDetailsResponse.getEmail(), idamUserIdDetailsResponse);
    }

    @Test
    public void getUserDetailsThrowsUserNotFoundExceptionTest() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        ResponseEntity<IdamUserIdDetailsResponse> responseEntity = null;

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdDetailsResponse.class)
        )).thenReturn(responseEntity);

        Exception exception = Assertions.assertThrows(UserNotFoundException.class, () ->
            idamService.getUserDetails(header));
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("Internal Server error. Please, try again later"));
    }

    @Test(expected = UserNotFoundException.class)
    public void getUserDetailsThrowsHttpClientErrorExceptionTest() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdDetailsResponse.class)
        )).thenThrow(HttpClientErrorException.class);
        idamService.getUserDetails(header);
    }

    @Test(expected = GatewayTimeoutException.class)
    public void getUserDetailsThrowsHttpServerErrorExceptionTest() {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdDetailsResponse.class)
        )).thenThrow(HttpServerErrorException.class);
        idamService.getUserDetails(header);
    }
}
