package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.reports.FeesRegisterClient;
import uk.gov.hmcts.payment.api.reports.FeesService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

}
