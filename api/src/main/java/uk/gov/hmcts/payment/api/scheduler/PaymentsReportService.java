package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.controllers.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.email.CardPaymentReconciliationReportEmail;
import uk.gov.hmcts.payment.api.email.EmailFailedException;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.CardPaymentService;
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

    private static final String CSV_FILE_PREFIX = "hmcts_payments_";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private static final String HEADER = "Service,PaymentOld reference,CCD reference,Case reference,PaymentOld date,PaymentOld channel,PaymentOld amount,"
        + "Site id,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version,Fee code,Version";

    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    private CardPaymentDtoMapper cardPaymentDtoMapper;

    private EmailService emailService;

    private CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail;

    @Autowired
    public PaymentsReportService(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardPaymentService, CardPaymentDtoMapper cardPaymentDtoMapper,
                                 EmailService emailService, CardPaymentReconciliationReportEmail cardPaymentReconciliationReportEmail) {
        this.cardPaymentService = cardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.emailService = emailService;
        this.cardPaymentReconciliationReportEmail = cardPaymentReconciliationReportEmail;
    }

    public void generateCsv(String startDate, String endDate) throws ParseException, IOException {

        Date fromDate = startDate == null ? sdf.parse(getYesterdaysDate()) : sdf.parse(startDate);
        Date toDate = endDate == null ? sdf.parse(getTodaysDate()) : sdf.parse(endDate);


        List<CardPaymentDto> cardPayments = cardPaymentService.search(fromDate, toDate).stream()
            .map(cardPaymentDtoMapper::toReconciliationResponseDto).collect(Collectors.toList());

        createCsv(cardPayments);

    }

    private void createCsv(List<CardPaymentDto> cardPayments) throws IOException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String fileNameSuffix = LocalDateTime.now().format(formatter);
        String csvFileName = CSV_FILE_PREFIX + fileNameSuffix + ".csv";

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(HEADER.getBytes());
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            for (CardPaymentDto cardPayment : cardPayments) {
                bos.write(cardPayment.toCsv().getBytes());
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes());
            }

            LOG.info("PaymentsReportService - Total " + cardPayments.size() + " records written in payments csv file " + csvFileName);

            cardPaymentReconciliationReportEmail.setAttachments(newArrayList(csv(bos.toByteArray(), csvFileName)));

            emailService.sendEmail(cardPaymentReconciliationReportEmail);

            LOG.info("PaymentsReportService - Card payments report  email sent to " + Arrays.toString(cardPaymentReconciliationReportEmail.getTo()));

        } catch (IOException | EmailFailedException ex) {

            LOG.error("PaymentsReportService - Error while creating card payments csv file " + csvFileName + ". Error message is" + ex.getMessage());

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
