package uk.gov.hmcts.payment.api.reports;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestDto;
import uk.gov.hmcts.payment.api.model.DuplicateServiceRequestKey;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.reports.config.ServiceRequestReportConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;

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
    private final ServiceRequestReportConfig reportConfig;

    @Autowired
    public ServiceRequestReportService(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                       EmailService emailService,
                                       ServiceRequestReportConfig reportConfig) {
        this.emailService = emailService;
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.reportConfig = reportConfig;
    }

    public void generateDuplicateSRCsvAndSendEmail(LocalDate date) {

        LOG.info("Start of duplicate service request csv report");

        Optional<List<DuplicateServiceRequestDto>> duplicateServiceRequestDtos = paymentFeeLinkRepository.getDuplicates(date);

        Map<DuplicateServiceRequestKey, Integer> duplicateServiceRequestMap = new HashMap<>();
        if (duplicateServiceRequestDtos.isPresent()) {

            List<DuplicateServiceRequestDto> dtos = duplicateServiceRequestDtos.get();
            for (DuplicateServiceRequestDto dto : dtos) {
                DuplicateServiceRequestKey key = DuplicateServiceRequestKey.builder()
                    .ccd(dto.getCcd_case_number())
                    .service(dto.getEnterprise_service_name())
                    .feeCodes(dto.getFee_codes())
                    .build();
                if (duplicateServiceRequestMap.containsKey(key)) {
                    duplicateServiceRequestMap.put(key, duplicateServiceRequestMap.get(key) + 1);
                } else {
                    duplicateServiceRequestMap.put(key, 1);
                }
            }
        }

        duplicateServiceRequestMap.entrySet().removeIf(entry -> entry.getValue() == 1);

        if (!duplicateServiceRequestMap.isEmpty()) {
            String paymentsCsvFileName = ServiceRequestReportConfig.DUPLICATE_SR_CSV_FILE_PREFIX + "." + date + FILE_EXTENSION;
            byte[] paymentsByteArray = createDuplicateSRCsvByteArray(duplicateServiceRequestMap, paymentsCsvFileName, reportConfig);
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
        LOG.info("ServiceRequestReportService - report email sent to {}", Arrays.toString(email.getTo()));
    }

    private byte[] createDuplicateSRCsvByteArray(Map<DuplicateServiceRequestKey, Integer> duplicateServiceRequestMap, String csvFileName, ServiceRequestReportConfig reportConfig) {
        byte[] csvByteArray = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(ServiceRequestReportConfig.DUPLICATE_SR_HEADER.getBytes(utf8));
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            for (DuplicateServiceRequestKey key : duplicateServiceRequestMap.keySet()) {
                bos.write(reportConfig.getDuplicateSRCsvRecord(key, duplicateServiceRequestMap.get(key)).getBytes(utf8));
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            }
            LOG.info("ServiceRequestReportService - Total {} duplicate service request records written in csv file {}", duplicateServiceRequestMap.size(), csvFileName);
            csvByteArray = bos.toByteArray();
        } catch (IOException ex) {
            LOG.error("ServiceRequestReportService - Error while creating csv file {}. Error message is {}", csvFileName, ex.getMessage());
        }
        return csvByteArray;
    }
}

