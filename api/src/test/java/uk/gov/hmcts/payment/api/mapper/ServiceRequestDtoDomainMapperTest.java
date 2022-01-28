package uk.gov.hmcts.payment.api.mapper;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestDtoDomainMapper;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestOnlinePaymentBo;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

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

        serviceRequestDtoDomainMapper.toDomain(serviceRequestDto,organisationalServiceDto);

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

        serviceRequestDtoDomainMapper.toDomain(onlineCardPaymentRequest,"http://returnUrl/","");

    }

    @Test
    public void createGovPayRequestTest() {

        ServiceRequestOnlinePaymentBo serviceRequestOnlinePaymentBo = ServiceRequestOnlinePaymentBo.serviceRequestOnlinePaymentBo()
            .amount(new BigDecimal(99.99))
            .description("desc")
            .returnUrl("http://returnUrl")
            .language("Eng")
            .paymentReference("RC-ref")
            .build();

        serviceRequestDtoDomainMapper.createGovPayRequest(serviceRequestOnlinePaymentBo);

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
