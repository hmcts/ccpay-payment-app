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
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        startDate =  sdf.parse(getYesterdaysDate("dd-MM-yyyy")) ;
        endDate = sdf.parse(getTodaysDate("dd-MM-yyyy"));

        paymentsReportService = new PaymentsReportService(cardPaymentService,cardPaymentDtoMapper,
            creditAccountPaymentService,creditAccountDtoMapper,emailService,feesService,cardPaymentReconciliationReportEmail,creditAccountReconciliationReportEmail);

    }




    @Test
    public void shouldGenerateCardPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(null,null);

        // then
        verify(cardPaymentService).search(startDate,endDate, PaymentMethodUtil.CARD.name());
        verify(emailService).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test
    public void shouldGenerateCreditAccountPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(null,null);

        // then
        verify(creditAccountPaymentService).search(startDate,endDate);
        verify(emailService).sendEmail(creditAccountReconciliationReportEmail);


    }

    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCardPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(getYesterdaysDate("dd-MM-yyyy"),getYesterdaysDate("dd-MM-yyyy"));

        // then
        verify(cardPaymentService,times(0)).search(startDate,startDate, PaymentMethodUtil.CARD.name());
        verify(feesService,times(0)).getFeeVersion(anyString(),anyString());
        verify(emailService,times(0)).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCardPaymentsCsvWhenStartDateGreaterThanEndDate()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(getTodaysDate("dd-MM-yyyy"),getYesterdaysDate("dd-MM-yyyy"));

        // then
        verify(cardPaymentService,times(0)).search(endDate,startDate, PaymentMethodUtil.CARD.name());
        verify(feesService,times(0)).getFeeVersion(anyString(),anyString());
        verify(emailService,times(0)).sendEmail(cardPaymentReconciliationReportEmail);


    }



    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getYesterdaysDate("dd-MM-yyyy"),getYesterdaysDate("dd-MM-yyyy"));

        // then
        verify(creditAccountPaymentService,times(0)).search(startDate,startDate);
        verify(feesService,times(0)).getFeeVersion(anyString(),anyString());
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }


    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenInCorrectStartDateFormat()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate("dd-MM-yyyy"),getYesterdaysDate("yyyy-MM-dd"));

        // then
        verify(creditAccountPaymentService,times(0)).search(endDate,startDate);
        verify(feesService,times(0)).getFeeVersion(anyString(),anyString());
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }


    @Test(expected = PaymentException.class)
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenInCorrectEndDateFormat()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate("yyyy-MM-dd"),getYesterdaysDate("dd-MM-yyyy"));

        // then
        verify(creditAccountPaymentService,times(0)).search(endDate,startDate);
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }

    private String getYesterdaysDate(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return sdf.format(mtDtNow.toDate());
    }

    private String getTodaysDate(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date());
    }



}
