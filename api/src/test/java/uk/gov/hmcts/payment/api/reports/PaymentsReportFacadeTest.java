
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
import uk.gov.hmcts.payment.api.reports.config.*;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.util.Date;
import java.util.Map;


import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;

import static org.mockito.Mockito.*;


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

    @Mock
    private Map<PaymentReportType, PaymentReportConfig> configMap;

    @Mock
    private PaymentReportConfig paymentReportConfig;

    @Mock
    private PaymentsReportFacade paymentsReportFacade;

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
    private DuplicatePaymentReportConfig duplicatePaymentReportConfig = new DuplicatePaymentReportConfig("from", null, "subject", "message", true);


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
        .put(PaymentReportType.PBA_SMC, pbaSmcPaymentReportConfig)
        .put(PaymentReportType.DUPLICATE_PAYMENT, duplicatePaymentReportConfig).build();

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
    public void PbaCmcDelegatePaymentReportType() {

        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // given & when

        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Civil Money Claims");

        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Civil Money Claims", pbaCmcPaymentReportConfig);
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
    public void PbaSmcDelegatePaymentReportType() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateCsvAndSendEmail(fromDate, toDate, PBA, "Specified Money Claims");

        // then
        verify(reportService).generateCsvAndSendEmail(fromDate, toDate, PBA, "Specified Money Claims", pbaSmcPaymentReportConfig);

    }

    @Test
    public void DuplicatePaymentReportType() {
        // given
        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        facade.generateDuplicatePaymentCsvAndSendEmail(fromDate, toDate);

        // then
        verify(reportService).generateDuplicatePaymentsCsvAndSendEmail(fromDate, toDate, duplicatePaymentReportConfig);

    }

    @Test
    public void testGenerateCsvAndSendEmail_WhenReportIsEnabled() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();
        PaymentMethodType paymentMethodType = PaymentMethodType.CARD;
        String serviceType = "serviceType";


       // when(configMap.get(PaymentReportType.from(paymentMethodType, serviceType))).thenReturn(paymentReportConfig);



        // Enable the report in the config
        when(paymentReportConfig.isEnabled()).thenReturn(true);

        // Act
        paymentsReportFacade.generateCsvAndSendEmail(startDate, endDate, paymentMethodType, serviceType);

        // Assert
        verify(reportService, times(1)).generateCsvAndSendEmail(
            eq(startDate),
            eq(endDate),
            eq(paymentMethodType),
            eq(serviceType),
            eq(paymentReportConfig)
        );
    }


    @Test
    public void testGenerateCsvAndSendEmail_WhenReportIsDisabled() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();
        PaymentMethodType paymentMethodType = PaymentMethodType.CARD;
        String serviceType = "service-type";

        // Set up the config map to return the mock PaymentReportConfig
       // when(configMap.get(PaymentReportType.from(paymentMethodType, serviceType))).thenReturn(paymentReportConfig);

        // Disable the report in the config
        when(paymentReportConfig.isEnabled()).thenReturn(false);

        // Act
        paymentsReportFacade.generateCsvAndSendEmail(startDate, endDate, paymentMethodType, serviceType);

        // Assert
        verify(reportService, never()).generateCsvAndSendEmail(any(), any(), any(), any(), any());
    }

    @Test
    public void testGenerateDuplicatePaymentCsvAndSendEmail_WhenReportIsEnabled() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();

        // Set up the config map to return the mock PaymentReportConfig for duplicate payments
       // when(configMap.get(PaymentReportType.DUPLICATE_PAYMENT)).thenReturn(paymentReportConfig);

        // Enable the report in the config
        when(paymentReportConfig.isEnabled()).thenReturn(true);

        // Act
        paymentsReportFacade.generateDuplicatePaymentCsvAndSendEmail(startDate, endDate);

        // Assert
        verify(reportService, times(1)).generateDuplicatePaymentsCsvAndSendEmail(
            eq(startDate),
            eq(endDate),
            eq(paymentReportConfig)
        );
    }

    @Test
    public void testGenerateDuplicatePaymentCsvAndSendEmail_WhenReportIsDisabled() {
        // Arrange
        Date startDate = new Date();
        Date endDate = new Date();

        // Set up the config map to return the mock PaymentReportConfig for duplicate payments
        when(configMap.get(PaymentReportType.DUPLICATE_PAYMENT)).thenReturn(paymentReportConfig);

        // Disable the report in the config
        when(paymentReportConfig.isEnabled()).thenReturn(false);

        // Act
        paymentsReportFacade.generateDuplicatePaymentCsvAndSendEmail(startDate, endDate);

        // Assert
        verify(reportService, never()).generateDuplicatePaymentsCsvAndSendEmail(any(), any(), any());
    }


}
