package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.SupplementaryDetailsResponse;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IacServiceTest {

    @Mock
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Mock
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private IacServiceImpl iacService;

    static private String IAC_SERVICE_NAME = "IAC";

    PaymentDto paymentDto;

    @Before
    public void setUp() {
        paymentDto =  PaymentDto.payment2DtoWith()
            .paymentReference("RC-2222-3333-4444-5555")
            .ccdCaseNumber("1111-2222-3333-4444")
            .caseReference(null)
            .serviceName(IAC_SERVICE_NAME)
            .amount(BigDecimal.valueOf(1)).build();

        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getIacSupplementaryInfoSuccess() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        ResponseEntity<SupplementaryDetailsResponse> responseEntity = new ResponseEntity<>(new SupplementaryDetailsResponse(), HttpStatus.OK);
        when(restTemplateIacSupplementaryInfo.exchange(anyString(), any(), any(), eq(SupplementaryDetailsResponse.class)))
            .thenReturn(responseEntity);

        ResponseEntity<SupplementaryPaymentDto> result = iacService.getIacSupplementaryInfo(paymentDtos, IAC_SERVICE_NAME);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    public void getIacSupplementaryInfoHttpClientErrorException() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        when(restTemplateIacSupplementaryInfo.exchange(anyString(), any(), any(), eq(SupplementaryDetailsResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        ResponseEntity<SupplementaryPaymentDto> result = iacService.getIacSupplementaryInfo(paymentDtos, IAC_SERVICE_NAME);

        assertEquals(HttpStatus.PARTIAL_CONTENT, result.getStatusCode());
    }

    @Test
    public void updateCaseReferenceInPaymentDtosSuccess() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1111-2222-3333-4444").caseReference("IAC/1234/REF").build();
        when(paymentFeeLinkRepository.findByCcdCaseNumber("1111-2222-3333-4444")).thenReturn(Optional.of(Collections.singletonList(paymentFeeLink)));

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_NAME);

        assertEquals("IAC/1234/REF", paymentDto.getCaseReference());
    }

    @Test
    public void updateCaseReferenceInPaymentDtosMultipleFeeLinksSuccess() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        PaymentFeeLink paymentFeeLink1 = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1111-2222-3333-4444").build();
        PaymentFeeLink paymentFeeLink2 = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1111-2222-3333-4444").build();
        PaymentFeeLink paymentFeeLink3 = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1111-2222-3333-4444").caseReference("IAC/1234/REF").build();
        PaymentFeeLink paymentFeeLink4 = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1111-2222-3333-4444").build();

        List<PaymentFeeLink> paymentFeeLinks = List.of(paymentFeeLink1, paymentFeeLink2, paymentFeeLink3, paymentFeeLink4);
        when(paymentFeeLinkRepository.findByCcdCaseNumber("1111-2222-3333-4444")).thenReturn(Optional.of(paymentFeeLinks));

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_NAME);

        assertEquals("IAC/1234/REF", paymentDto.getCaseReference());
    }

    @Test
    public void updateCaseReferenceInPaymentDtosIgnoredAsAlreadyPopulated() {
        paymentDto.setCaseReference("IAC/1234/REF");
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        PaymentFeeLink paymentFeeLink = new PaymentFeeLink();
        paymentFeeLink.setCaseReference("IAC/6789/REF");
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.of(Collections.singletonList(paymentFeeLink)));

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_NAME);

        assertEquals("IAC/1234/REF", paymentDto.getCaseReference());
    }

    @Test
    public void updateCaseReferenceInPaymentDtosNoCaseReference() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.empty());

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_NAME);

        assertNull(paymentDto.getCaseReference());
    }
}
