package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CsvExtractServiceTest {

    @Mock
    private CsvExtractService csvExtractService;


    private CsvExtractScheduler scheduler;

    @Before
    public void setUp() {
        scheduler = new CsvExtractScheduler(csvExtractService,null,null);
    }

    @Test
    public void shouldInvokeCsvExtractServiceToExtractCsv() throws Exception {
        // given

        // when
        scheduler.extractCsv();

        // then
        verify(csvExtractService).extractCsv(null,null);
    }
}








