package uk.gov.hmcts.payment.api.mapper;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.domain.mapper.ServiceRequestPaymentDtoDomainMapper;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.math.BigDecimal;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestPaymentDtoDomainMapperTest {

    @InjectMocks
    ServiceRequestPaymentDtoDomainMapper serviceRequestPaymentDtoDomainMapper;

    @Mock
    ReferenceUtil referenceUtil;

    @Test
    public void toDomainTest() throws CheckDigitException {

        ServiceRequestPaymentDto paymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100))
            .currency("GBP")
            .customerReference("123456")
            .build();

        Mockito.when(referenceUtil.getNext("RC")).thenReturn("RC-ref");

        serviceRequestPaymentDtoDomainMapper.toDomain(paymentDto);
    }
}
