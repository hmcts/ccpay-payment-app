package uk.gov.hmcts.payment.api.mapper;

import lombok.val;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.liberata.FeeRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentByAccountRequest;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.IacService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class PBAPaymentMapper {

    private static final Logger LOG = LoggerFactory.getLogger(PBAStatusErrorMapper.class);

    @Autowired
    private FeesService feesService;

    @Autowired
    private final PaymentService<PaymentFeeLink, String> paymentService;

    @Autowired
    private IacService iacService;

    public PBAPaymentMapper(PaymentService<PaymentFeeLink, String> paymentService, FeesService feesService, IacService iacService) {
        this.paymentService = paymentService;
        this.feesService = feesService;
        this.iacService = iacService;
    }

    public PaymentAccountRequest mapToPaymentByAccountRequest(CreditAccountPaymentRequest creditAccountPaymentRequest, String groupReference, Payment payment) {
        val paymentDto = mapPaymentAccountPayment(creditAccountPaymentRequest, groupReference,payment);
        val  paymentByAccountRequest = PaymentByAccountRequest.paymentByAccountRequestWith()
            .pbaNumber(creditAccountPaymentRequest.getAccountNumber())
            .payment(paymentDto)
            .build();

        return PaymentAccountRequest.paymentAccountRequestWith()
            .PaymentByAccountRequest(paymentByAccountRequest).build();
    }

    private boolean isACService(String serviceCode) {
        return serviceCode.equalsIgnoreCase("IAC");
    }

    private String getSurname(Payment payment, CreditAccountPaymentRequest creditAccountPaymentRequest, String paymentReference) {
        if ( isACService(payment.getServiceType()) ){
            val  iacServiceName = paymentService.getServiceNameByCode(payment.getServiceType());
            val paymentDto =  getPaymentDtoFromModel(payment, paymentReference, creditAccountPaymentRequest);
            try {
                val iacResponse = iacService.getIacSupplementaryInfo(List.of(paymentDto), iacServiceName);

                if (iacResponse == null
                    || iacResponse.getBody() == null
                    || iacResponse.getBody().getSupplementaryInfo() == null
                    || iacResponse.getBody().getSupplementaryInfo().isEmpty()
                    || iacResponse.getBody().getSupplementaryInfo().getFirst() == null
                    || iacResponse.getBody().getSupplementaryInfo().getFirst().getSupplementaryDetails() == null
                    || iacResponse.getBody().getSupplementaryInfo().getFirst().getSupplementaryDetails().getSurname() == null
                    || iacResponse.getBody().getSupplementaryInfo().getFirst().getSupplementaryDetails().getSurname().trim().isEmpty()) {
                    LOG.info("IAC response missing surname for payment reference: {}", paymentReference);
                    return "Not applicable, the servide type is not IAC";
                }
                return iacResponse.getBody().getSupplementaryInfo().getFirst().getSupplementaryDetails().getSurname();

            } catch (Exception exception) {
                LOG.error("Error occurred while fetching supplementary info from IAC service for payment reference: {}", paymentReference, exception);
                return "Not applicable, the servide type is not IAC";
            }
        } else {
            return "Not applicable, the servide type is not IAC";
        }
    }


    public PaymentDto getPaymentDtoFromModel(Payment payment, String paymentReference, CreditAccountPaymentRequest creditAccountPaymentReques) {

        val paymentDto = PaymentDto.payment2DtoWith()
            .paymentReference(payment.getReference())
            .paymentGroupReference(paymentReference)
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .amount(payment.getAmount())
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .accountNumber(payment.getPbaNumber())
            .organisationName(payment.getOrganisationName())
            .customerReference(payment.getCustomerReference())
            .channel(payment.getPaymentChannel() != null ? payment.getPaymentChannel().getName() : null)
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .status(payment.getPaymentStatus().getName())
            .dateCreated(payment.getDateCreated())
            .dateUpdated(payment.getDateUpdated())
            .method(payment.getPaymentMethod().getName())
            .bankedDate(payment.getBankedDate())
            .giroSlipNo(payment.getGiroSlipNo())
            .externalProvider(payment.getPaymentProvider() != null ? payment.getPaymentProvider().getName() : null)
            .externalReference(payment.getExternalReference())
            .fees(creditAccountPaymentReques.getFees())
            .build();
        return paymentDto;
    }

    private PaymentRequest mapPaymentAccountPayment(CreditAccountPaymentRequest creditAccountPaymentRequest, String groupReference,Payment payment) {
        val surname = getSurname(payment, creditAccountPaymentRequest, groupReference);
        return PaymentRequest.paymentDtoWith()
            .groupReference(groupReference)
            .paymentReference(groupReference)
            .surname(surname)
            .serviceName(creditAccountPaymentRequest.getService())
            .amount(creditAccountPaymentRequest.getAmount().toString())
            .currency(creditAccountPaymentRequest.getCurrency().getCode())
            .siteId(creditAccountPaymentRequest.getSiteId())
            .caseReference(creditAccountPaymentRequest.getCaseReference())
            .ccdCaseNumber(creditAccountPaymentRequest.getCcdCaseNumber())
            .customerReference(creditAccountPaymentRequest.getCustomerReference())
            .fee(mapPaymentAccountFee(creditAccountPaymentRequest))
            .dateCreated(new Date())
            .build();
    }

    private List<FeeRequest> mapPaymentAccountFee(CreditAccountPaymentRequest creditAccountPaymentRequest) {

        return creditAccountPaymentRequest.getFees().stream().map(feeDto -> {

            val feeVersionDto = populateFeeDetails(feeDto.getCode(), feeDto.getVersion());
            val fee2Dto = getFee2Dto(feeDto.getCode());

            return FeeRequest.feeWith()
                .code(feeDto.getCode())
                .id(1102)
                .version(feeDto.getVersion())
                .memoline(feeVersionDto.getMemoLine())
                .nac(feeVersionDto.getNaturalAccountCode())
                .jurisdiction1(fee2Dto.getJurisdiction1Dto().getName())
                .jurisdiction2(fee2Dto.getJurisdiction1Dto().getName())
                .volume(feeDto.getVolume().toString())
                .calculatedAmount(feeDto.getCalculatedAmount().toString())
                .build();
        }).toList();
    }


    private FeeVersionDto populateFeeDetails(String feeCode, String version) {
        val optionalFeeVersionDto =feesService.getFeeVersion(feeCode,version);
       return optionalFeeVersionDto.orElseThrow(()-> new PaymentException("Fee details not found for feeCode: " + feeCode + " and version: " + version));
    }

    private Fee2Dto  getFee2Dto(String feeCode){
        val optFrFeeMap = Optional.ofNullable(feesService.getFeesDtoMap());
        if (optFrFeeMap.isPresent() && optFrFeeMap.get().containsKey(feeCode)) {
            val frFeeMap = optFrFeeMap.get();
            return frFeeMap.get(feeCode);
        } else {
            throw new PaymentException("Fee details not found for feeCode: " + feeCode);
        }
    }
}
