package uk.gov.hmcts.payment.api.scheduler;

import com.google.common.collect.ImmutableMap;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.reports.FeesRegisterClient;
import uk.gov.hmcts.payment.api.reports.FeesService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.fees2.register.api.contract.Fee2Dto.fee2DtoWith;

@RunWith(MockitoJUnitRunner.class)
public class FeesServiceTest {

    private FeesService feesService;

    @Mock
    private FeesRegisterClient feesRegisterClient;

    private List<PaymentDto>  payments = Arrays.asList(PaymentDto.payment2DtoWith()
        .fees(Arrays.asList(FeeDto.feeDtoWith()
            .code("X0001")
            .version("1").build())).build());


    @Before
    public void setUp()  {
        feesService = new FeesService(feesRegisterClient);
        }



    @Test
    public void shouldGetFeesDtoMap()  {
        // given

        // when
        feesService.getFeesDtoMap();

        // then
        verify(feesRegisterClient,times(1)).getFeesDataAsMap();

    }

    @Test
    public void shouldGetFeesVersionsData()  {
        // given

        // when
        feesService.getFeesVersionsData();

        // then
        verify(feesRegisterClient,times(1)).getFeesDataAsMap();

    }

    @Test
    public void shouldGetEmptyFeeVersion_whenClientThrowsException()  {
        // given
        given(feesRegisterClient.getFeesDataAsMap()).willThrow(new RuntimeException());
        // when
        Optional<FeeVersionDto> feeVersion = feesService.getFeeVersion("FEE001", "2");

        // then
        assertThat(feeVersion).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldGetEmptyFeeVersion_whenRuntimeExceptionThrownDueToPartialData()  {
        // given feeCode without currentVersion
        Map<String,Fee2Dto>  versionMap = ImmutableMap.of("2", fee2DtoWith().code("FEE001").build());
        given(feesRegisterClient.getFeesDataAsMap()).willReturn(Optional.of(versionMap));
        // when
        Optional<FeeVersionDto> feeVersion = feesService.getFeeVersion("FEE001", "2");

        // then
        assertThat(feeVersion).isEqualTo(Optional.empty());
    }

}
