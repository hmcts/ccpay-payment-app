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

    static private String IAC_SERVICE_CODE = "IAC";

    PaymentDto paymentDto;

    @Before
    public void setUp() {
        paymentDto =  PaymentDto.payment2DtoWith()
            .paymentGroupReference("2024-1706099566733")
            .paymentReference("RC-2222-3333-4444-5555")
            .ccdCaseNumber("1111-2222-3333-4444")
            .caseReference(null)
            .serviceName(IAC_SERVICE_CODE)
            .amount(BigDecimal.valueOf(1)).build();

        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getIacSupplementaryInfoSuccess() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        ResponseEntity<SupplementaryDetailsResponse> responseEntity = new ResponseEntity<>(new SupplementaryDetailsResponse(), HttpStatus.OK);
        when(restTemplateIacSupplementaryInfo.exchange(anyString(), any(), any(), eq(SupplementaryDetailsResponse.class)))
            .thenReturn(responseEntity);

        ResponseEntity<SupplementaryPaymentDto> result = iacService.getIacSupplementaryInfo(paymentDtos, IAC_SERVICE_CODE);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    public void getIacSupplementaryInfoHttpClientErrorException() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        when(restTemplateIacSupplementaryInfo.exchange(anyString(), any(), any(), eq(SupplementaryDetailsResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        ResponseEntity<SupplementaryPaymentDto> result = iacService.getIacSupplementaryInfo(paymentDtos, IAC_SERVICE_CODE);

        assertEquals(HttpStatus.PARTIAL_CONTENT, result.getStatusCode());
    }

    @Test
    public void updateCaseReferenceInPaymentDtoFeeLinksSuccess() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("2024-1706099566733").caseReference("IAC/1234/REF").build();
        when(paymentFeeLinkRepository.findByPaymentReference("2024-1706099566733")).thenReturn(Optional.of(paymentFeeLink));

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_CODE);

        assertEquals("IAC/1234/REF", paymentDto.getCaseReference());
    }

    @Test
    public void updateCaseReferenceInPaymentDtosIgnoredAsAlreadyPopulated() {
        paymentDto.setCaseReference("IAC/1234/REF");
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().caseReference("IAC/6789/REF").build();
        when(paymentFeeLinkRepository.findByPaymentReference(anyString())).thenReturn(Optional.of(paymentFeeLink));

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_CODE);

        assertEquals("IAC/1234/REF", paymentDto.getCaseReference());
    }


    @Test
    public void updateCaseReferenceInPaymentDtosEmptyCaseReference() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().paymentReference("2024-1706099566733").caseReference("").build();
        when(paymentFeeLinkRepository.findByPaymentReference("2024-1706099566733")).thenReturn(Optional.of(paymentFeeLink));

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_CODE);

        assertEquals(null, paymentDto.getCaseReference());
    }

    @Test
    public void updateCaseReferenceInPaymentDtosNoCaseReference() {
        List<PaymentDto> paymentDtos = Collections.singletonList(paymentDto);
        when(paymentFeeLinkRepository.findByCcdCaseNumber(anyString())).thenReturn(Optional.empty());

        iacService.updateCaseReferenceInPaymentDtos(paymentDtos, IAC_SERVICE_CODE);

        assertNull(paymentDto.getCaseReference());
    }
}
