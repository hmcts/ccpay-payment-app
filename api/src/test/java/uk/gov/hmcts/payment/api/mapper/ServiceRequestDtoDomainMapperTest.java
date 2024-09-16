package uk.gov.hmcts.payment.api.mapper;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestBo;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestDtoDomainMapperTest {

    @InjectMocks
    ServiceRequestDtoDomainMapper serviceRequestDtoDomainMapper;

    @Mock
    ReferenceUtil referenceUtil;

    @Mock
    UserIdSupplier userIdSupplier;

    @Mock
    ServiceIdSupplier serviceIdSupplier;

    @Mock
    PaymentReference paymentReference;

    @Test
    public void toDomainTest(){

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(Collections.singletonList(getServiceRequestFee()))
            .build();

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AA001")
            .serviceDescription("DIVORCE")
            .build();

        Mockito.when(paymentReference.getNext()).thenReturn("2024-1234567890");

        ServiceRequestBo serviceRequestBo = serviceRequestDtoDomainMapper.toDomain(serviceRequestDto,organisationalServiceDto);

        assertTrue(serviceRequestBo.getOrgId().equals("AA001"));
        assertTrue(serviceRequestBo.getEnterpriseServiceName().equals("DIVORCE"));
        assertTrue(serviceRequestBo.getCcdCaseNumber().equals("8689869686968696"));
        assertTrue(serviceRequestBo.getReference().equals("2024-1234567890"));
        assertTrue(serviceRequestBo.getFees().get(0).getNetAmount().equals(getServiceRequestFee().getCalculatedAmount()));

    }

    @Test
    public void toDomainTest2() throws CheckDigitException {

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .language("Eng")
            .currency(CurrencyCode.GBP)
            .amount(new BigDecimal(99.99).setScale(2, RoundingMode.HALF_EVEN))
            .build();

        Mockito.when(referenceUtil.getNext("RC")).thenReturn("RC-ref");

        Mockito.when(userIdSupplier.get()).thenReturn("userID");

        Mockito.when(serviceIdSupplier.get()).thenReturn("s2sServiceName");

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = serviceRequestDtoDomainMapper.toDomain(onlineCardPaymentRequest,"http://returnUrl/","");

        assertTrue(serviceRequestOnlinePaymentBo.getUserId().equals("userID"));
        assertTrue(serviceRequestOnlinePaymentBo.getS2sServiceName().equals("s2sServiceName"));
        assertTrue(serviceRequestOnlinePaymentBo.getLanguage().equals("eng"));
        assertEquals(serviceRequestOnlinePaymentBo.getAmount(), BigDecimal.valueOf(99.99));
    }

    @Test
    public void createGovPayRequestTest() {

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .amount(new BigDecimal(250))
            .description("desc")
            .returnUrl("http://returnUrl")
            .language("en")
            .paymentReference("RC-ref")
            .build();

        CreatePaymentRequest govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);

        assertTrue(govPayRequest.getDescription().equals("desc"));
        assertTrue(govPayRequest.getReference().equals("RC-ref"));
        assertTrue(govPayRequest.getReturnUrl().equals("http://returnUrl?language=en"));
        assertEquals(govPayRequest.getAmount(), Integer.valueOf(25000));
        assertTrue(govPayRequest.getLanguage().equals("en"));

    }

    @Test
    public void createGovPayRequestWelshLanguageTest() {

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .amount(new BigDecimal(99.99))
            .description("desc")
            .returnUrl("http://returnUrl")
            .language("cy")
            .paymentReference("RC-ref")
            .build();

        CreatePaymentRequest govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);

        assertTrue(govPayRequest.getDescription().equals("desc"));
        assertTrue(govPayRequest.getReference().equals("RC-ref"));
        assertTrue(govPayRequest.getReturnUrl().equals("http://returnUrl?language=cy"));
        assertEquals(govPayRequest.getAmount(), Integer.valueOf(9998));
        assertTrue(govPayRequest.getLanguage().equals("cy"));

    }

    @Test
    public void createGovPayRequestWithEnglishDefaultLanguageTest() {

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .amount(new BigDecimal(99.99))
            .description("desc")
            .returnUrl("http://returnUrl")
            .paymentReference("RC-ref")
            .build();

        // Language null
        serviceRequestOnlinePaymentBo.setLanguage(null);
        CreatePaymentRequest govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);
        assertTrue(govPayRequest.getLanguage().equals("en"));

        // Language '' (empty)
        serviceRequestOnlinePaymentBo.setLanguage("");
        govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);
        assertTrue(govPayRequest.getLanguage().equals("en"));

        // Language 'string'
        serviceRequestOnlinePaymentBo.setLanguage("string");
        govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);
        assertTrue(govPayRequest.getLanguage().equals("en"));

        // Language 'EN'
        serviceRequestOnlinePaymentBo.setLanguage("EN");
        govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);
        assertTrue(govPayRequest.getLanguage().equals("en"));

        // Language 'English'
        serviceRequestOnlinePaymentBo.setLanguage("English!");
        govPayRequest = serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);
        assertTrue(govPayRequest.getLanguage().equals("en"));

    }

    private ServiceRequestFeeDto getServiceRequestFee() {
        return ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }
}
