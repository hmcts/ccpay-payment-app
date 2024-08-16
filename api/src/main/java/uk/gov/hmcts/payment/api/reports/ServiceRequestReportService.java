package uk.gov.hmcts.payment.api.reports;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.reports.config.DuplicateServiceRequestReportConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static com.google.common.collect.Lists.newArrayList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.payment.api.email.EmailAttachment.csv;

@org.springframework.stereotype.Service
public class ServiceRequestReportService {

    private static final Logger LOG = getLogger(ServiceRequestReportService.class);
    private static final String BYTE_ARRAY_OUTPUT_STREAM_NEWLINE = "\r\n";
    private static final String FILE_EXTENSION = ".csv";
    private static final Charset utf8 = Charset.forName("UTF-8");
    private final EmailService emailService;
    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final DuplicateServiceRequestReportConfig reportConfig;

    @Autowired
    public ServiceRequestReportService(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                       EmailService emailService,
                                       DuplicateServiceRequestReportConfig reportConfig) {
        this.emailService = emailService;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.reportConfig = reportConfig;
    }

    public void generateCsvAndSendEmail(LocalDate date){

        LOG.info("Start of duplicate service request csv report");

        Optional<List<DuplicateServiceRequestDto>> duplicateServiceRequestDtos = paymentFeeLinkRepository.getDuplicates(date);
        if (duplicateServiceRequestDtos.isPresent()){
            String paymentsCsvFileName = DuplicateServiceRequestReportConfig.CSV_FILE_PREFIX + "." + date + FILE_EXTENSION;
            byte[] paymentsByteArray = createCsvByteArray(duplicateServiceRequestDtos.get(), paymentsCsvFileName, reportConfig);
            Email email = Email.emailWith()
                .from(reportConfig.getFrom())
                .to(reportConfig.getTo())
                .subject(reportConfig.getSubject())
                .message(reportConfig.getMessage())
                .build();
            sendEmail(email, paymentsByteArray, paymentsCsvFileName);
        };

        LOG.info("End of duplicate service request csv report");
    }


    private void sendEmail(Email email, byte[] paymentsCsvByteArray, String csvFileName) {
        email.setAttachments(newArrayList(csv(paymentsCsvByteArray, csvFileName)));
        emailService.sendEmail(email);
        LOG.info("PaymentsReportService - Payments report email sent to {}", Arrays.toString(email.getTo()));
    }

    private byte[] createCsvByteArray(List<DuplicateServiceRequestDto> duplicateServiceRequestDtos, String csvFileName, DuplicateServiceRequestReportConfig reportConfig) {
        byte[] csvByteArray = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(DuplicateServiceRequestReportConfig.HEADER.getBytes(utf8));
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            for (DuplicateServiceRequestDto duplicateServiceRequestDto : duplicateServiceRequestDtos) {
                bos.write(reportConfig.getCsvRecord(duplicateServiceRequestDto).getBytes(utf8));
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            }
            LOG.info("DuplicateServiceRequestReportService - Total {} duplicate service request records written in payments csv file {}", duplicateServiceRequestDtos.size(), csvFileName);
            csvByteArray = bos.toByteArray();
        } catch (IOException ex) {
            LOG.error("DuplicateServiceRequestReportService - Error while creating payments csv file {}. Error message is {}", csvFileName, ex.getMessage());
        }
        return csvByteArray;
    }
}

