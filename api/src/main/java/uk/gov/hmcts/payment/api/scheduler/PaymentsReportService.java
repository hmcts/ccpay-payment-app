package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.controllers.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.controllers.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.email.CardPaymentReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.CreditAccountReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.EmailFailedException;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.CardPaymentService;
import uk.gov.hmcts.payment.api.model.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.payment.api.email.EmailAttachment.csv;

@Service
@Transactional
public class PaymentsReportService {

    private static final Logger LOG = getLogger(PaymentsReportService.class);

    private static final String BYTE_ARRAY_OUTPUT_STREAM_NEWLINE = "\r\n";

    private static final String CARD_PAYMENTS_CSV_FILE_PREFIX = "hmcts_card_payments_";

    private static final String PBA_PAYMENTS_CSV_FILE_PREFIX = "hmcts_pba_payments_";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private static final String CARD_PAYMENTS_HEADER = "Service,Payment reference,CCD reference,Case reference,Payment date,Payment channel,Payment amount,"
        + "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private static final String PBA_PAYMENTS_HEADER = "Service,Payment Group reference,CCD reference,Case reference,Payment date,Payment channel,Payment amount,"
        + "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    private CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;

    private CardPaymentDtoMapper cardPaymentDtoMapper;
    private CreditAccountDtoMapper creditAccountDtoMapper;

    private EmailService emailService;

    private CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;
    private CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail;


    @Autowired
    public PaymentsReportService(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardPaymentService, CardPaymentDtoMapper cardPaymentDtoMapper,
                                 @Qualifier("loggingCreditAccountPaymentService")CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService,CreditAccountDtoMapper creditAccountDtoMapper,
                                 EmailService emailService, CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail, CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail1) {
        this.cardPaymentService = cardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.emailService = emailService;
        this.cardPaymentReconciliationReportEmail = cardPaymentReconciliationReportEmail;
        this.creditAccountReconciliationReportEmail = creditAccountReconciliationReportEmail1;
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
    }

    public void generateCardPaymentsCsv(String startDate, String endDate) throws ParseException, IOException {

        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);


        List<PaymentDto> cardPayments = cardPaymentService.search(fromDate, toDate).stream()
            .map(cardPaymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        createCardPaymentsCsv(cardPayments);

    }

    private void createCardPaymentsCsv(List<PaymentDto> cardPayments) throws IOException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String cardPaymentsCsvFileName = CARD_PAYMENTS_CSV_FILE_PREFIX + fileNameSuffix + ".csv";

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(CARD_PAYMENTS_HEADER.getBytes());
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            for (PaymentDto cardPayment : cardPayments) {
                bos.write(cardPayment.toCsv().getBytes());
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            }

            LOG.info("PaymentsReportService - Total " + cardPayments.size() + " card payments records written in payments csv file " + cardPaymentsCsvFileName);

            cardPaymentReconciliationReportEmail.setAttachments(newArrayList(csv(bos.toByteArray(), cardPaymentsCsvFileName)));

            emailService.sendEmail(cardPaymentReconciliationReportEmail);

            LOG.info("PaymentsReportService - Card payments report  email sent to " + Arrays.toString(cardPaymentReconciliationReportEmail.getTo()));

        } catch (IOException | EmailFailedException ex) {

            LOG.error("PaymentsReportService - Error while creating card payments csv file " + cardPaymentsCsvFileName + ". Error message is" + ex.getMessage());

        }


    }

    public void generateCreditAccountPaymentsCsv(String startDate, String endDate) throws ParseException, IOException {

        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);

        System.out.println(startDate);
        System.out.println(endDate);
        List<PaymentDto> creditAccountPayments = creditAccountPaymentService.search(fromDate, toDate).stream()
            .map(creditAccountDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        createCreditAccountPaymentsCsv(creditAccountPayments);

    }

    private void createCreditAccountPaymentsCsv(List<PaymentDto> creditAccountPayments) throws IOException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String creditAccountCsvFileName = PBA_PAYMENTS_CSV_FILE_PREFIX + fileNameSuffix + ".csv";

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(PBA_PAYMENTS_HEADER.getBytes());
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            for (PaymentDto creditAccountPayment : creditAccountPayments) {
                bos.write(creditAccountPayment.toCsv().getBytes());
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            }

            LOG.info("PaymentsReportService - Total " +creditAccountPayments.size() + " credit account payments(pba) records written in payments csv file " + creditAccountCsvFileName);

            creditAccountReconciliationReportEmail.setAttachments(newArrayList(csv(bos.toByteArray(), creditAccountCsvFileName)));

            emailService.sendEmail(creditAccountReconciliationReportEmail);

            LOG.info("PaymentsReportService - Credit account payments report  email sent to " + Arrays.toString(creditAccountReconciliationReportEmail.getTo()));

        } catch (IOException | EmailFailedException ex) {

            LOG.error("PaymentsReportService - Error while creating credit account payments csv file " + creditAccountCsvFileName + ". Error message is" + ex.getMessage());

        }


    }

    private String getYesterdaysDate() {
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return sdf.format(mtDtNow.toDate());
    }

    private String getTodaysDate() {
        return sdf.format(new Date());
    }

}
