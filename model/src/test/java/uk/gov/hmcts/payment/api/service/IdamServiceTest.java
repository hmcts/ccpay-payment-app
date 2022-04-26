package uk.gov.hmcts.payment.api.service;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.dto.IdamFullNameRetrivalResponse;
import uk.gov.hmcts.payment.api.dto.IdamUserIdResponse;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.exceptions.UserNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
public class IdamServiceTest{

    @InjectMocks
    private IdamServiceImpl idamService;

    @Mock
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;

    public static final String IDAM_USER_ID = "1f2b7025-0f91-4737-92c6-b7a9baef14c6";

    public static final Supplier<IdamFullNameRetrivalResponse[]> idamFullNameCCDSearchRefundListSupplier = () -> new IdamFullNameRetrivalResponse[]{IdamFullNameRetrivalResponse
        .idamFullNameRetrivalResponseWith()
        .id(IDAM_USER_ID)
        .email("mockfullname@gmail.com")
        .forename("mock-Forename")
        .surname("mock-Surname")
        .roles(List.of("Refund-approver", "Refund-admin"))
        .build()};

    @Test
    public void getResponseOnValidToken() throws Exception {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        IdamUserIdResponse mockIdamUserIdResponse = IdamUserIdResponse.idamUserIdResponseWith()

            .id("123")
            .forename("VP")
            .surname("VP")
            .email("V_P@gmail.com")
            .active("true")
            .roles(Arrays.asList("vp"))
            .build();

        ResponseEntity<IdamUserIdResponse> responseEntity = new ResponseEntity<>(mockIdamUserIdResponse, HttpStatus.OK);

        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamUserIdResponse.class)
        )).thenReturn(responseEntity);

        String idamUserIdResponse = idamService.getUserId(header);
        assertEquals(mockIdamUserIdResponse.getEmail(), idamUserIdResponse);
    }

    @Test
    public void getResponseOnValidToken1() throws Exception {

        MultiValueMap<String, String> header1 = new LinkedMultiValueMap<String, String>();
        header1.put("authorization", Collections.singletonList("Bearer 131313"));

        UserIdentityDataDto mockIdamUserIdResponse1 = UserIdentityDataDto.userIdentityDataWith()
            .fullName("abc")
            .emailId("mockfullname@gmail.com")
            .build();

        mockIdamFullNameCall(IDAM_USER_ID, idamFullNameCCDSearchRefundListSupplier.get());

        UserIdentityDataDto idamUserIdResponse = idamService.getUserIdentityData(header1,"123445");
        assertEquals(mockIdamUserIdResponse1.getEmailId(), idamUserIdResponse.getEmailId());
    }

    @Test
    public void getExceptionOnInvalidToken() throws Exception {

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
    public void getExceptionOnTokenReturnsNullResponse() throws Exception {

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
    public void testGetUserFullNameExceptionScenarios() {
        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        header.put("authorization", Collections.singletonList("Bearer 131313"));

        ResponseEntity<IdamFullNameRetrivalResponse[]> responseForFullNameCCDUserId =
            new ResponseEntity<>(idamFullNameCCDSearchRefundListSupplier.get(), HttpStatus.OK);
        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamFullNameRetrivalResponse[].class)
        )).thenThrow(new UserNotFoundException("User Not Found"));

        assertThrows(UserNotFoundException.class, () -> {
            idamService.getUserIdentityData(header, IDAM_USER_ID);
        });
    }

    @Test
    public void validateResponseDto() throws Exception {
        IdamUserIdResponse idamUserIdResponse = IdamUserIdResponse.idamUserIdResponseWith()
            .id("123")
            .forename("VP")
            .surname("VP")
            .email("V_P@gmail.com")
            .active("true")
            .roles(Arrays.asList("vp"))
            .build();

        assertEquals("123", idamUserIdResponse.getId());
        assertEquals("VP", idamUserIdResponse.getForename());
        assertEquals("VP", idamUserIdResponse.getSurname());
        assertEquals(Arrays.asList("vp"), idamUserIdResponse.getRoles());
        assertEquals("V_P@gmail.com", idamUserIdResponse.getEmail());
    }

    public void mockIdamFullNameCall(String userId,
                                     IdamFullNameRetrivalResponse[] idamFullNameRetrivalResponse) {
        UriComponentsBuilder builderCCDSearchURI = UriComponentsBuilder.fromUriString("http://localhost" + "def")
            .queryParam("query", "id:" + userId);
        ResponseEntity<IdamFullNameRetrivalResponse[]> responseForFullNameCCDUserId =
            new ResponseEntity<>(idamFullNameRetrivalResponse, HttpStatus.OK);
        when(restTemplateIdam.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(IdamFullNameRetrivalResponse[].class)
        )).thenReturn(responseForFullNameCCDUserId);
    }

}

