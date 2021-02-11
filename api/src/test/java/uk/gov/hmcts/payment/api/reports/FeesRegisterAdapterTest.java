package uk.gov.hmcts.payment.api.reports;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FeesRegisterAdapterTest {

    @Mock
    FeesRegisterClient feesRegisterClient;

    @InjectMocks
    FeesRegisterAdapter feesRegisterAdapter = new FeesRegisterAdapter(feesRegisterClient);

    Map<String, Fee2Dto> fee2DtoMapMock = new HashMap<String, Fee2Dto>();

    @Before
    public void initiate(){
        Fee2Dto fee2Dto = Fee2Dto.fee2DtoWith()
                            .code("FEE123").build();
        fee2DtoMapMock.put("fee",fee2Dto);
    }

    @Test
    public void testGetFeesDtoMap(){
        when(feesRegisterClient.getFeesDataAsMap()).thenReturn(Optional.ofNullable(fee2DtoMapMock));
        Map<String, Fee2Dto> fee2DtoMap = feesRegisterAdapter.getFeesDtoMap();
        assertEquals("FEE123",fee2DtoMap.get("fee").getCode());
    }
}
