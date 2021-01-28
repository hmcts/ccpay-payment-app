package uk.gov.hmcts.payment.api.reports;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.reports.config.BarPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaCmcPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaFplPaymentReportConfig;

import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.CARD;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.PBA;

@RunWith(MockitoJUnitRunner.class)
public class PaymentsReportFacadeTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private PaymentsReportFacade facade;

    @Mock
    private PaymentsReportService reportService;

    private CardPaymentReportConfig cardPaymentReportConfig = new CardPaymentReportConfig("from", null, "subject", "message", true);
    private BarPaymentReportConfig barPaymentReportConfig = new BarPaymentReportConfig("from", null, "subject", "message", true);
    private PbaCmcPaymentReportConfig pbaCmcPaymentReportConfig = new PbaCmcPaymentReportConfig("from", null, "subject", "message", false);
    private PbaFplPaymentReportConfig pbaFplPaymentReportConfig = new PbaFplPaymentReportConfig("from", null, "subject", "message", true);

    @Before
    public void setUp() {
        Map<PaymentReportType, PaymentReportConfig> map = ImmutableMap.of(PaymentReportType.CARD, cardPaymentReportConfig, PaymentReportType.PBA_CMC, pbaCmcPaymentReportConfig,
            PaymentReportType.DIGITAL_BAR, barPaymentReportConfig, PaymentReportType.PBA_FPL, pbaFplPaymentReportConfig);
        facade = new PaymentsReportFacade(reportService, map);
    }

    @Test
    public void shouldDelegateToServiceIfEnabled() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, CARD, null);

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, CARD, null, cardPaymentReportConfig);
    }

    @Test
    public void shouldDelegateToServiceIfExistingConfigurationForService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, null, "DIGITAL BAR");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, null, "DIGITAL BAR", barPaymentReportConfig);
    }

    @Test
    public void shouldNotDelegateToServiceIfDisabled() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "CMC");

        // then
        verifyZeroInteractions(reportService);
    }

    @Test
    public void shouldThrowExceptionForInvalidPaymentReportType() {
        exception.expect(UnsupportedOperationException.class);

        // given & when
        facade.generateCsvAndSendEmail(new Date(), new Date(), CARD,"Civil Money Claims");

    }

    @Test
    public void shouldThrowExceptionForEmptyValuesForMethodTypeAndService() {
        exception.expect(UnsupportedOperationException.class);

        // given & when
        facade.generateCsvAndSendEmail(new Date(), new Date(), null, null);
    }

    @Test
    public void fplDelegateFPLConfigurationService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Family Public Law");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Family Public Law", pbaFplPaymentReportConfig);

    }

}
