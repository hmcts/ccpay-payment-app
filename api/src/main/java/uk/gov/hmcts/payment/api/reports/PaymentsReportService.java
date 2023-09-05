package uk.gov.hmcts.payment.api.reports;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.payment.api.dto.DuplicatePaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.email.Email;
import uk.gov.hmcts.payment.api.email.EmailService;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.config.PaymentReportConfig;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.payment.api.email.EmailAttachment.csv;

@org.springframework.stereotype.Service
public class PaymentsReportService {

    private static final Logger LOG = getLogger(PaymentsReportService.class);

    private static final String BYTE_ARRAY_OUTPUT_STREAM_NEWLINE = "\r\n";

    private static final String PAYMENTS_CSV_FILE_EXTENSION = ".csv";

    private static final Charset utf8 = Charset.forName("UTF-8");

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private final Payment2Repository paymentRepository;
    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;


    private final PaymentDtoMapper paymentDtoMapper;
    private final EmailService emailService;
    private final FeesService feesService;

    @Autowired
    public PaymentsReportService(@Qualifier("loggingPaymentService") DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                                 Payment2Repository paymentRepository, PaymentDtoMapper paymentDtoMapper,
                                 EmailService emailService, FeesService feesService) {
        this.delegatingPaymentService = delegatingPaymentService;
        this.paymentRepository = paymentRepository;
        this.paymentDtoMapper = paymentDtoMapper;
        this.emailService = emailService;
        this.feesService = feesService;
    }

    public void generateCsvAndSendEmail(Date startDate, Date endDate, PaymentMethodType paymentMethodType, String serviceName,
                                        PaymentReportConfig reportConfig) {

        LOG.info("Start of payments csv report for method type :{} and service name :{}", paymentMethodType, serviceName);

        List<PaymentDto> cardPaymentsCsvData = findPaymentsBy(startDate, endDate, paymentMethodType, serviceName);
        generateCsvAndSendEmail(cardPaymentsCsvData, reportConfig);

        LOG.info("End of payments csv report for method type :{} and service name :{}", paymentMethodType, serviceName);
    }

    public void generateDuplicatePaymentsCsvAndSendEmail(Date startDate, Date endDate, PaymentReportConfig reportConfig) {

        LOG.info("Start of duplicate payments csv report");

        List<DuplicatePaymentDto> duplicatePaymentsCsvData = findDuplicatePaymentsBy(startDate, endDate);
        generateDuplicatePaymentReportCsvAndSendEmail(duplicatePaymentsCsvData, reportConfig);

        LOG.info("End of duplicate payments csv report");
    }

    private List<PaymentDto> findPaymentsBy(Date startDate, Date endDate, PaymentMethodType paymentMethodType, String serviceName) {
        String serviceType = serviceName;
        String paymentMethodTypeString = Optional.ofNullable(paymentMethodType).map(PaymentMethodType::getType).orElse(null);
        LOG.info("Inside findPaymentsBy - paymentMethodTypeString: {}", paymentMethodTypeString);
        return delegatingPaymentService
            .search(
                PaymentSearchCriteria.searchCriteriaWith()
                    .startDate(startDate)
                    .endDate(endDate)
                    .paymentMethod(paymentMethodTypeString)
                    .serviceType(serviceType)
                    .build()
            )
            .stream()
            .map(paymentDtoMapper::toReconciliationResponseDto)
            .collect(Collectors.toList());
    }

    private List<DuplicatePaymentDto> findDuplicatePaymentsBy(Date startDate, Date endDate) {
        LOG.info("Inside findDuplicatePaymentsBy");
        return paymentRepository.findDuplicatePaymentsByDate(startDate, endDate);
    }

    private void generateCsvAndSendEmail(List<PaymentDto> payments, PaymentReportConfig reportConfig) {
        LOG.info("CollectionUtils.isNotEmpty(payments): {}", CollectionUtils.isNotEmpty(payments));
        String paymentsCsvFileName = reportConfig.getCsvFileNamePrefix() + LocalDateTime.now().format(formatter) + PAYMENTS_CSV_FILE_EXTENSION;
        byte[] paymentsByteArray = createPaymentsCsvByteArray(payments, paymentsCsvFileName, reportConfig);
        Email email = Email.emailWith()
            .from(reportConfig.getFrom())
            .to(reportConfig.getTo())
            .subject(reportConfig.getSubject())
            .message(reportConfig.getMessage())
            .build();
        sendEmail(email, paymentsByteArray, paymentsCsvFileName);
    }

    private void generateDuplicatePaymentReportCsvAndSendEmail(List<DuplicatePaymentDto> duplicatePayments, PaymentReportConfig reportConfig) {
        LOG.info("CollectionUtils.isNotEmpty(duplicatePayments): {}", CollectionUtils.isNotEmpty(duplicatePayments));
        String paymentsCsvFileName = reportConfig.getCsvFileNamePrefix() + LocalDateTime.now().format(formatter) + PAYMENTS_CSV_FILE_EXTENSION;
        byte[] paymentsByteArray = createDuplicatePaymentsCsvByteArray(duplicatePayments, paymentsCsvFileName, reportConfig);
        Email email = Email.emailWith()
            .from(reportConfig.getFrom())
            .to(reportConfig.getTo())
            .subject(reportConfig.getSubject())
            .message(reportConfig.getMessage())
            .build();
        sendEmail(email, paymentsByteArray, paymentsCsvFileName);
    }

    private void sendEmail(Email email, byte[] paymentsCsvByteArray, String csvFileName) {
        email.setAttachments(newArrayList(csv(paymentsCsvByteArray, csvFileName)));
        emailService.sendEmail(email);
        LOG.info("PaymentsReportService - Payments report email sent to {}", Arrays.toString(email.getTo()));
    }

    private byte[] createPaymentsCsvByteArray(List<PaymentDto> payments, String paymentsCsvFileName, PaymentReportConfig reportConfig) {
        byte[] paymentsCsvByteArray = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(reportConfig.getCsvHeader().getBytes(utf8));
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            for (PaymentDto payment : payments) {
                bos.write(reportConfig.getCsvRecord(payment).getBytes(utf8));
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            }
            LOG.info("PaymentsReportService - Total {} payments records written in payments csv file {}", payments.size(), paymentsCsvFileName);
            paymentsCsvByteArray = bos.toByteArray();
        } catch (IOException ex) {
            LOG.error("PaymentsReportService - Error while creating payments csv file {}. Error message is {}", paymentsCsvFileName, ex.getMessage());
        }
        return paymentsCsvByteArray;
    }

    private byte[] createDuplicatePaymentsCsvByteArray(List<DuplicatePaymentDto> duplicatePayments, String paymentsCsvFileName, PaymentReportConfig reportConfig) {
        byte[] paymentsCsvByteArray = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bos.write(reportConfig.getCsvHeader().getBytes(utf8));
            bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            for (DuplicatePaymentDto duplicatePayment : duplicatePayments) {
                bos.write(reportConfig.getCsvRecord(duplicatePayment).getBytes(utf8));
                bos.write(BYTE_ARRAY_OUTPUT_STREAM_NEWLINE.getBytes(utf8));
            }
            LOG.info("PaymentsReportService - Total {} duplicate payment records written in csv file {}", duplicatePayments.size(), paymentsCsvFileName);
            paymentsCsvByteArray = bos.toByteArray();
        } catch (IOException ex) {
            LOG.error("PaymentsReportService - Error while creating duplicate payments csv file {}. Error message is {}", paymentsCsvFileName, ex.getMessage());
        }
        return paymentsCsvByteArray;
    }
}

