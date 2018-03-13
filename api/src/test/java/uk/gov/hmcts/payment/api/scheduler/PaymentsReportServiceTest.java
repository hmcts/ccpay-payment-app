package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
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
import java.util.Map;

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
    private CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;

    @Mock
    private CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail;

    @Mock
    private Map<String,Fee2Dto> feesDataMap;

    private Date startDate;
    private Date endDate;

    @Before
    public void setUp() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        startDate =  sdf.parse(getYesterdaysDate("dd-MM-yyyy")) ;
        endDate = sdf.parse(getTodaysDate("dd-MM-yyyy"));

        paymentsReportService = new PaymentsReportService(cardPaymentService,cardPaymentDtoMapper,
            creditAccountPaymentService,creditAccountDtoMapper,emailService,cardPaymentReconciliationReportEmail,creditAccountReconciliationReportEmail);

    }




    @Test
    public void shouldGenerateCardPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(null,null,feesDataMap);

        // then
        verify(cardPaymentService).search(startDate,endDate);
        verify(emailService).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test
    public void shouldGenerateCreditAccountPaymentsCsvAndSendEmail()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(null,null,feesDataMap);

        // then
        verify(creditAccountPaymentService).search(startDate,endDate);
        verify(emailService).sendEmail(creditAccountReconciliationReportEmail);


    }

    @Test
    public void shouldNotGenerateCardPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(getYesterdaysDate("dd-MM-yyyy"),getYesterdaysDate("dd-MM-yyyy"),feesDataMap);

        // then
        verify(cardPaymentService,times(0)).search(startDate,startDate);
        verify(emailService,times(0)).sendEmail(cardPaymentReconciliationReportEmail);


    }

    @Test
    public void shouldNotGenerateCardPaymentsCsvWhenStartDateGreaterThanEndDate()  {
        // given

        // when
        paymentsReportService.generateCardPaymentsCsvAndSendEmail(getTodaysDate("dd-MM-yyyy"),getYesterdaysDate("dd-MM-yyyy"),feesDataMap);

        // then
        verify(cardPaymentService,times(0)).search(endDate,startDate);
        verify(emailService,times(0)).sendEmail(cardPaymentReconciliationReportEmail);


    }



    @Test
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenDatesGivenAreEqual()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getYesterdaysDate("dd-MM-yyyy"),getYesterdaysDate("dd-MM-yyyy"),feesDataMap);

        // then
        verify(creditAccountPaymentService,times(0)).search(startDate,startDate);
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }


    @Test
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenInCorrectStartDateFormat()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate("dd-MM-yyyy"),getYesterdaysDate("yyyy-MM-dd"),feesDataMap);

        // then
        verify(creditAccountPaymentService,times(0)).search(endDate,startDate);
        verify(emailService,times(0)).sendEmail(creditAccountReconciliationReportEmail);


    }
    @Test
    public void shouldNotGenerateCreditAccountPaymentsCsvWhenInCorrectEndDateFormat()  {
        // given

        // when
        paymentsReportService.generateCreditAccountPaymentsCsvAndSendEmail(getTodaysDate("yyyy-MM-dd"),getYesterdaysDate("dd-MM-yyyy"),feesDataMap);

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
