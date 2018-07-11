package uk.gov.hmcts.payment.api.componenttests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.payment.api.controllers.PaymentReportController;
import uk.gov.hmcts.payment.api.controllers.RestErrorHandler;
import uk.gov.hmcts.payment.api.exception.ValidationErrorException;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.scheduler.Clock;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class PaymentReportControllerMockTest {

    private static final Date FROM_DATE = new Date();
    private static final Date TO_DATE = new Date();

    private MockMvc mockMvc;

    @Mock
    private PaymentsReportService paymentsReportService;
    @Mock
    private PaymentValidator validator;
    @Mock
    private Clock clock;

    @InjectMocks
    private PaymentReportController controller;


    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestErrorHandler()).build();
    }

    @Test
    public void cardPaymentReportWithNoInputDates() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "cardReportsEnabled", true);
        given(clock.getYesterdayDate()).willReturn(FROM_DATE);
        given(clock.getTodayDate()).willReturn(TO_DATE);
        // when & then
        this.mockMvc.perform(post("/payments/email-pay-reports")
            .param("payment_method", "CARD"))
            .andExpect(status().isOk());

        verify(paymentsReportService).generateCardPaymentsCsvAndSendEmail(FROM_DATE, TO_DATE, null);
    }

    @Test
    public void cardPaymentReportWithInputDates() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "cardReportsEnabled", true);

        given(clock.atStartOfDay("2018-06-30", DateTimeFormatter.ISO_DATE)).willReturn(FROM_DATE);
        given(clock.atEndOfDay("2018-07-01", DateTimeFormatter.ISO_DATE)).willReturn(TO_DATE);

        // when & then
        this.mockMvc.perform(post("/payments/email-pay-reports")
            .param("payment_method", "CARD")
            .param("start_date", "2018-06-30")
            .param("end_date", "2018-07-01")
            .param("service_name", "divorce"))
            .andExpect(status().isOk());

        verify(paymentsReportService).generateCardPaymentsCsvAndSendEmail(FROM_DATE, TO_DATE, "Divorce");
    }

    @Test
    public void pbaPaymentReportWithNoInputDates() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "pbaReportsEnabled", true);
        given(clock.getYesterdayDate()).willReturn(FROM_DATE);
        given(clock.getTodayDate()).willReturn(TO_DATE);
        // when & then
        this.mockMvc.perform(post("/payments/email-pay-reports")
            .param("payment_method", "PBA"))
            .andExpect(status().isOk());

        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(FROM_DATE, TO_DATE, null);
    }

    @Test
    public void pbaPaymentReportWithInputDates() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "pbaReportsEnabled", true);
        given(clock.atStartOfDay("2018-06-30", DateTimeFormatter.ISO_DATE)).willReturn(FROM_DATE);
        given(clock.atEndOfDay("2018-07-01", DateTimeFormatter.ISO_DATE)).willReturn(TO_DATE);
        // when & then
        this.mockMvc.perform(post("/payments/email-pay-reports")
            .param("payment_method", "PBA")
            .param("start_date", "2018-06-30")
            .param("end_date", "2018-07-01")
            .param("service_name", "CMC"))
            .andExpect(status().isOk());

        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(FROM_DATE, TO_DATE, "Civil Money Claims");
    }

    @Test
    public void cardPaymentReport_shouldThrowValidationError() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "cardReportsEnabled", true);

        doThrow(new ValidationErrorException("validation failed", null))
            .when(validator).validate(Optional.of("CARD"), Optional.of("CMC"), Optional.of("2018-06-30"), Optional.of("2018-07-01"));
        // when & then
        this.mockMvc.perform(post("/payments/email-pay-reports")
            .param("payment_method", "CARD")
            .param("start_date", "2018-06-30")
            .param("end_date", "2018-07-01")
            .param("service_name", "CMC"))
            .andExpect(status().isBadRequest());

        verifyZeroInteractions(paymentsReportService);
    }
}
