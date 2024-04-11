
package uk.gov.hmcts.payment.api.reports;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.payment.api.reports.config.BarPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.CardPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaCivilPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaCmcPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaDivorcePaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaFinremPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaFplPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaPrlPaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaProbatePaymentReportConfig;
import uk.gov.hmcts.payment.api.reports.config.PbaSmcPaymentReportConfig;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.CARD;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.PBA;

@RunWith(MockitoJUnitRunner.class)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class PaymentsReportFacadeTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private PaymentsReportFacade facade;

    @Mock
    private PaymentsReportService reportService;

    private CardPaymentReportConfig cardPaymentReportConfig = new CardPaymentReportConfig("from", null, "subject", "message", true);
    private BarPaymentReportConfig barPaymentReportConfig = new BarPaymentReportConfig("from", null, "subject", "message", true);
    private PbaCmcPaymentReportConfig pbaCmcPaymentReportConfig = new PbaCmcPaymentReportConfig("from", null, "subject", "message", true);
    private PbaCivilPaymentReportConfig pbaCivilPaymentReportConfig = new PbaCivilPaymentReportConfig("from", null, "subject", "message", true);
    private PbaDivorcePaymentReportConfig pbaDivorcePaymentReportConfig = new PbaDivorcePaymentReportConfig("from", null, "subject", "message", true);
    private PbaFplPaymentReportConfig pbaFplPaymentReportConfig = new PbaFplPaymentReportConfig("from", null, "subject", "message", true);
    private PbaPrlPaymentReportConfig pbaPrlPaymentReportConfig = new PbaPrlPaymentReportConfig("from", null, "subject", "message", true);
    private PbaProbatePaymentReportConfig pbaProbatePaymentReportConfig = new PbaProbatePaymentReportConfig("from", null, "subject", "message", true);
    private PbaFinremPaymentReportConfig pbaFinremPaymentReportConfig = new PbaFinremPaymentReportConfig("from", null, "subject", "message", true);
    private PbaSmcPaymentReportConfig pbaSmcPaymentReportConfig = new PbaSmcPaymentReportConfig("from", null, "subject", "message", true);


    Map<PaymentReportType, PaymentReportConfig> map = ImmutableMap.<PaymentReportType, PaymentReportConfig>builder()
        .put(PaymentReportType.CARD, cardPaymentReportConfig)
        .put(PaymentReportType.PBA_CMC, pbaCmcPaymentReportConfig)
        .put(PaymentReportType.PBA_CIVIL, pbaCivilPaymentReportConfig)
        .put(PaymentReportType.DIGITAL_BAR, barPaymentReportConfig)
        .put(PaymentReportType.PBA_FPL, pbaFplPaymentReportConfig)
        .put(PaymentReportType.PBA_PRL, pbaPrlPaymentReportConfig)
        .put(PaymentReportType.PBA_DIVORCE, pbaDivorcePaymentReportConfig)
        .put(PaymentReportType.PBA_PROBATE, pbaProbatePaymentReportConfig)
        .put(PaymentReportType.PBA_FINREM, pbaFinremPaymentReportConfig)
        .put(PaymentReportType.PBA_SMC, pbaSmcPaymentReportConfig).build();

    @Before
    public void setUp() {
        facade = new PaymentsReportFacade(reportService, map);
    }

    @After
    public void tearDown() {
        facade = null;
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
        facade.generateCsvAndSendEmail(fromDate, toDate, null, "DIGITAL_BAR");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, null, "DIGITAL_BAR", barPaymentReportConfig);
    }

    @Test
    public void PbaCivilDelegatePaymentReportType() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Civil");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Civil", pbaCivilPaymentReportConfig);
    }


    @Test
    public void shouldThrowExceptionForInvalidPaymentReportType() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        exception.expect(UnsupportedOperationException.class);
        facade.generateCsvAndSendEmail(fromDate, toDate, null, null);
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

    @Test
    public void prlDelegateFPLConfigurationService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Family Private Law");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Family Private Law", pbaPrlPaymentReportConfig);

    }

    @Test
    public void PbaDivorceConfigurationService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Divorce");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Divorce", pbaDivorcePaymentReportConfig);

    }


    @Test
    public void PbaProbateConfigurationService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Probate");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Probate", pbaProbatePaymentReportConfig);

    }

    @Test
    public void PbaFinremConfigurationService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Financial Remedy");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Financial Remedy", pbaFinremPaymentReportConfig);

    }

    @Test
    public void prlSpecifiedMoneyClaimsConfigurationService() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Specified Money Claims");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Specified Money Claims", pbaSmcPaymentReportConfig);

    }

}
