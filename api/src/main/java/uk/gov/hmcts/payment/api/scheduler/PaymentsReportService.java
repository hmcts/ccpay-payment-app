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
import uk.gov.hmcts.payment.api.email.Email;
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

    private static final String CREDIT_ACCOUNT_PAYMENTS_CSV_FILE_PREFIX = "hmcts_credit_account_payments_";

    private static final String PAYMENTS_CSV_FILE_EXTENSION = ".csv";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private static final String CARD_PAYMENTS_HEADER = "Service,Payment Group reference,Payment reference,CCD reference,Case reference," +
        "Payment created date,Payment status updated date,Payment status,Payment channel,Payment method,Payment amount,"
        + "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private static final String CREDIT_ACCOUNT_PAYMENTS_HEADER = "Service,Payment Group reference,Payment reference,CCD reference,Case reference," +
        "Organisation name,Customer internal reference,PBA Number,Payment created date,Payment status updated date," +
         "Payment status,Payment channel,Payment method,Payment amount," +
         "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    private CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;

    private CardPaymentDtoMapper cardPaymentDtoMapper;
    private CreditAccountDtoMapper creditAccountDtoMapper;

    private EmailService emailService;

    private CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;
    private CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail;


    @Autowired
    public PaymentsReportService(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardPaymentService, CardPaymentDtoMapper cardPaymentDtoMapper,
                                 @Qualifier("loggingCreditAccountPaymentService") CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService, CreditAccountDtoMapper creditAccountDtoMapper,
                                 EmailService emailService, CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail, CreditAccountReconciliationReportEmail creditAccountReconciliationReportEmail1) {
        this.cardPaymentService = cardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.emailService = emailService;
        this.cardPaymentReconciliationReportEmail = cardPaymentReconciliationReportEmail;
        this.creditAccountReconciliationReportEmail = creditAccountReconciliationReportEmail1;
        this.creditAccountPaymentService = creditAccountPaymentService;
        this.creditAccountDtoMapper = creditAccountDtoMapper;
    }

    public void generateCardPaymentsCsvAndSendEmail(String startDate, String endDate) throws ParseException, IOException {

        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);

        List<PaymentDto> cardPayments = cardPaymentService.search(fromDate, toDate).stream()
            .map(cardPaymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        String cardPaymentCsvFileNameSuffix = LocalDateTime.now().format(formatter);
        String paymentsCsvFileName = CARD_PAYMENTS_CSV_FILE_PREFIX + cardPaymentCsvFileNameSuffix + PAYMENTS_CSV_FILE_EXTENSION;
        generateCsvAndSendEmail(cardPayments, paymentsCsvFileName, CARD_PAYMENTS_HEADER, cardPaymentReconciliationReportEmail);
    }

    public void generateCreditAccountPaymentsCsvAndSendEmail(String startDate, String endDate) throws ParseException, IOException {

        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);

        List<PaymentDto> creditAccountPayments = creditAccountPaymentService.search(fromDate, toDate).stream()
            .map(creditAccountDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String paymentsCsvFileName = CREDIT_ACCOUNT_PAYMENTS_CSV_FILE_PREFIX + fileNameSuffix + PAYMENTS_CSV_FILE_EXTENSION;
        generateCsvAndSendEmail(creditAccountPayments, paymentsCsvFileName, CREDIT_ACCOUNT_PAYMENTS_HEADER, creditAccountReconciliationReportEmail);
    }

    public void generateCsvAndSendEmail(List<PaymentDto> payments, String paymentsCsvFileName, String header, Email mail) {

        byte[] paymentsByteArray = createPaymentsCsvByteArray(payments, paymentsCsvFileName, header);
        sendEmail(mail, paymentsByteArray, paymentsCsvFileName);

    }

    public byte[] createPaymentsCsvByteArray(List<PaymentDto> payments, String paymentsCsvFileName, String header) {

        byte[] paymentsCsvByteArray = null;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(header.getBytes());
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            for (PaymentDto payment : payments) {
                if (paymentsCsvFileName.startsWith(CARD_PAYMENTS_CSV_FILE_PREFIX)) {
                    bos.write(payment.toCardPaymentCsv().getBytes());
                } else if (paymentsCsvFileName.startsWith(CREDIT_ACCOUNT_PAYMENTS_CSV_FILE_PREFIX)){
                    bos.write(payment.toCreditAccountPaymentCsv().getBytes());
                }
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            }

            LOG.info("PaymentsReportService - Total " + payments.size() + " payments records written in payments csv file " + paymentsCsvFileName);

            paymentsCsvByteArray = bos.toByteArray();

        } catch (IOException ex) {

            LOG.error("PaymentsReportService - Error while creating card payments csv file " + paymentsCsvFileName + ". Error message is" + ex.getMessage());

        }
        return paymentsCsvByteArray;

    }

    public void sendEmail(Email email, byte[] paymentsCsvByteArray, String csvFileName) {
        email.setAttachments(newArrayList(csv(paymentsCsvByteArray, csvFileName)));
        emailService.sendEmail(email);

        LOG.info("PaymentsReportService - Payments report email sent to " + Arrays.toString(email.getTo()));

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
