package uk.gov.hmcts.payment.api.scheduler;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.reports.FeesRegisterAdapter;
import uk.gov.hmcts.payment.api.reports.FeesRegisterClient;
import uk.gov.hmcts.payment.api.reports.FeesService;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.fees2.register.api.contract.Fee2Dto.fee2DtoWith;

@RunWith(MockitoJUnitRunner.class)
public class FeesServiceTest {

    private FeesService feesService;

    private FeesRegisterAdapter feesRegisterAdapter;

    @Mock
    private FeesRegisterClient feesRegisterClient;


    @Before
    public void setUp() {
        feesRegisterAdapter = new FeesRegisterAdapter(feesRegisterClient);
        feesService = new FeesService(feesRegisterAdapter);
    }

    @After
    public void tearDown() {
        feesRegisterAdapter = null;
        feesService = null;
    }

    @Test
    public void shouldGetFeesDtoMap() {
        // given

        // when
        feesService.getFeesDtoMap();

        // then
        verify(feesRegisterClient, times(1)).getFeesDataAsMap();

    }

    @Test
    public void shouldGetEmptyFeeVersion_whenClientThrowsException() {
        // given
        given(feesRegisterClient.getFeesDataAsMap()).willThrow(new RuntimeException());
        // when
        Optional<FeeVersionDto> feeVersion = feesService.getFeeVersion("FEE001", "2");

        // then
        assertThat(feeVersion).isEmpty();
    }

    @Test
    public void shouldGetEmptyFeeVersion_whenRuntimeExceptionThrownDueToPartialData() {
        // given feeCode without currentVersion
        Map<String, Fee2Dto> versionMap = ImmutableMap.of("2", fee2DtoWith().code("FEE001").build());
        given(feesRegisterClient.getFeesDataAsMap()).willReturn(Optional.of(versionMap));
        // when
        Optional<FeeVersionDto> feeVersion = feesService.getFeeVersion("FEE001", "2");

        // then
        assertThat(feeVersion).isEmpty();
    }

    @Test
    public void shouldHandleNullFromFeesDtoMap() {

        when(feesRegisterClient.getFeesDataAsMap()).thenReturn(Optional.empty());
        assertThat(feesService.getFeesVersionsData()).isEmpty();

        when(feesRegisterClient.getFeesDataAsMap()).thenReturn(Optional.of(Collections.emptyMap())) ;
        assertThat(feesService.getFeesVersionsData()).isEmpty();
    }

}
