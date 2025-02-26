package uk.gov.hmcts.payment.api.domain.mapper;

import org.junit.Test;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestFeeBo;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceRequestDtoDomainMapperTest {

    private final ServiceRequestDtoDomainMapper mapper = new ServiceRequestDtoDomainMapper();

    @Test
    public void testToFeeDomainWithNonNullVolume() {
        ServiceRequestFeeDto feeDto = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100.00"))
            .code("FEE001")
            .version("1")
            .volume(5)
            .build();

        ServiceRequestFeeBo feeBo = mapper.toFeeDomain(feeDto, "123456789");

        assertThat(feeBo.getCalculatedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(feeBo.getCode()).isEqualTo("FEE001");
        assertThat(feeBo.getCcdCaseNumber()).isEqualTo("123456789");
        assertThat(feeBo.getVersion()).isEqualTo("1");
        assertThat(feeBo.getVolume()).isEqualTo(5);
        assertThat(feeBo.getNetAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    public void testToFeeDomainWithNullVolume() {
        ServiceRequestFeeDto feeDto = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100.00"))
            .code("FEE001")
            .version("1")
            .volume(null)
            .build();

        ServiceRequestFeeBo feeBo = mapper.toFeeDomain(feeDto, "123456789");

        assertThat(feeBo.getCalculatedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(feeBo.getCode()).isEqualTo("FEE001");
        assertThat(feeBo.getCcdCaseNumber()).isEqualTo("123456789");
        assertThat(feeBo.getVersion()).isEqualTo("1");
        assertThat(feeBo.getVolume()).isEqualTo(1); // Default volume to 1
        assertThat(feeBo.getNetAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
