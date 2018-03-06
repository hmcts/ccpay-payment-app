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
import uk.gov.hmcts.payment.api.service.CardPaymentService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private CardPaymentDtoMapper cardPaymentDtoMapper;

    @Mock
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @Mock
    CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;

    @Mock
    CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail;

    private Date startDate;
    private Date endDate;

    @Before
    public void setUp() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        startDate =  sdf.parse(getYesterdaysDate()) ;
        endDate = sdf.parse(getTodaysDate());

        paymentsReportService = new PaymentsReportService(cardPaymentService,cardPaymentDtoMapper,
            creditAccountPaymentService,creditAccountDtoMapper,emailService,cardPaymentReconciliationReportEmail,creditAccountReconciliationReportEmail);

    }




    @Test
    public void shouldGenerateCardPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(null,null);

        // then
        verify(cardPaymentService).search(startDate,endDate);
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

    @Test
    public void shouldNotGenerateCardPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(getYesterdaysDate(),getYesterdaysDate());

        // then
        verify(cardPaymentService,times(0)).search(startDate,startDate);
        verify(emailService,times(0)).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test
    public void shouldNotGenerateCardPaymentsCsvWhenStartDateGreaterThanEndDate()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(getTodaysDate(),getYesterdaysDate());

        // then
        verify(cardPaymentService,times(0)).search(endDate,startDate);
        verify(emailService,times(0)).sendEmail(cardPaymentReconciliationReportEmail);


    }



    @Test
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getYesterdaysDate(),getYesterdaysDate());

        // then
        verify(creditAccountPaymentService,times(0)).search(startDate,startDate);
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }
    @Test
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenStartDateGreaterThanEndDate()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate(),getYesterdaysDate());

        // then
        verify(creditAccountPaymentService,times(0)).search(endDate,startDate);
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }





    private String getYesterdaysDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return sdf.format(mtDtNow.toDate());
    }

    private String getTodaysDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.format(new Date());
    }



}
