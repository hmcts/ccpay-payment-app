package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.dto.mapper.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.email.CardPaymentReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.CreditAccountReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PaymentsReportServiceTest {


    private PaymentsReportService paymentsReportService;

    @Mock
    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    @Mock
    private CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;

    @Mock
    private EmailService emailService;

    @Mock
    private FeesService feesService;

    @Mock
    private CardPaymentDtoMapper cardPaymentDtoMapper;

    @Mock
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @Mock
    private CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;

    @Mock
    private CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail;


    private Date startDate;
    private Date endDate;

    @Before
    public void setUp() throws ParseException {

        startDate = getYesterdaysDate();
        endDate = getTodaysDate();

        paymentsReportService = new PaymentsReportService(cardPaymentService,cardPaymentDtoMapper,
            creditAccountPaymentService,creditAccountDtoMapper,emailService,feesService,cardPaymentReconciliationReportEmail,creditAccountReconciliationReportEmail);

    }

    @Test
    public void shouldGenerateCardPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(startDate,endDate);

        // then
        verify(cardPaymentService).search(startDate,endDate, "card", null);
        verify(emailService).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test
    public void shouldGenerateCreditAccountPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(startDate,endDate);

        // then
        verify(creditAccountPaymentService).search(startDate,endDate);
        verify(emailService).sendEmail(creditAccountReconciliationReportEmail);


    }

    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getYesterdaysDate(),getYesterdaysDate());

        // then
        verify(creditAccountPaymentService,times(0)).search(startDate,startDate);
        verify(feesService,times(0)).getFeeVersion(anyString(),anyString());
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }


    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenInCorrectStartDateFormat()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate(),getYesterdaysDate());

        // then
        verify(creditAccountPaymentService,times(0)).search(endDate,startDate);
        verify(feesService,times(0)).getFeeVersion(anyString(),anyString());
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }


    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenInCorrectEndDateFormat()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate(),getYesterdaysDate());

        // then
        verify(creditAccountPaymentService,times(0)).search(endDate,startDate);
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }

    private Date getYesterdaysDate() {
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return mtDtNow.toDate();
    }

    private Date getTodaysDate() {
        return new Date();
    }

}
