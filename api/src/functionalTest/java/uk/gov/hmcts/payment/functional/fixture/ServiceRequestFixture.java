package uk.gov.hmcts.payment.functional.fixture;

import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ServiceRequestFixture {

    public static final ServiceRequestDto buildServiceRequestDTO(final String hmctsOrgId,
                                                                 final String ccdCaseNumber) {

        CasePaymentRequest casePaymentRequest = CasePaymentRequest
            .casePaymentRequestWith()
            .responsibleParty("Party 1")
            .action("Action 1").build();

        ServiceRequestDto serviceRequestDto = ServiceRequestDto
            .serviceRequestDtoWith()
            .casePaymentRequest(casePaymentRequest)
            .callBackUrl("http://callback.hmcts.net")
            .hmctsOrgId(hmctsOrgId)
            .caseReference("123245677")
            .ccdCaseNumber(ccdCaseNumber != null ? ccdCaseNumber : generateUniqueCCDCaseReferenceNumber()).fees(List.of(getFee())).build();
        return serviceRequestDto;
    }

    public static final ServiceRequestDto buildServiceRequestDTOWithMultipleFees(final String hmctsOrgId,
                                                                 final String ccdCaseNumber) {

        CasePaymentRequest casePaymentRequest = CasePaymentRequest
            .casePaymentRequestWith()
            .responsibleParty("Party 1")
            .action("Action 1").build();

        ServiceRequestDto serviceRequestDto = ServiceRequestDto
            .serviceRequestDtoWith()
            .casePaymentRequest(casePaymentRequest)
            .callBackUrl("http://callback.hmcts.net")
            .hmctsOrgId(hmctsOrgId)
            .caseReference("123245677")
            .ccdCaseNumber(ccdCaseNumber != null ? ccdCaseNumber : generateUniqueCCDCaseReferenceNumber())
            .fees(getMultipleFees()).build();
        return serviceRequestDto;
    }

    public static final ServiceRequestDto buildServiceRequestDTOWithDuplicateFees(final String hmctsOrgId,
                                                                                 final String ccdCaseNumber) {

        CasePaymentRequest casePaymentRequest = CasePaymentRequest
            .casePaymentRequestWith()
            .responsibleParty("Party 1")
            .action("Action 1").build();

        ServiceRequestDto serviceRequestDto = ServiceRequestDto
            .serviceRequestDtoWith()
            .casePaymentRequest(casePaymentRequest)
            .callBackUrl("http://callback.hmcts.net")
            .hmctsOrgId(hmctsOrgId)
            .caseReference("123245677")
            .ccdCaseNumber(ccdCaseNumber != null ? ccdCaseNumber : generateUniqueCCDCaseReferenceNumber())
            .fees(getDuplicateFees()).build();
        return serviceRequestDto;
    }





    private static final ServiceRequestFeeDto getFee() {
        return ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100.00"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }

    private static final List<ServiceRequestFeeDto> getMultipleFees() {
        ServiceRequestFeeDto fee1 = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100"))
            .code("FEE100")
            .version("1")
            .volume(1)
            .build();

        ServiceRequestFeeDto fee2 = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("200"))
            .code("FEE102")
            .version("1")
            .volume(1)
            .build();

        return List.of(fee1, fee2);
    }

    private static final List<ServiceRequestFeeDto> getDuplicateFees() {

        ServiceRequestFeeDto fee1 = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100"))
            .code("FEE100")
            .version("1")
            .volume(1)
            .build();

        ServiceRequestFeeDto fee2 = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100"))
            .code("FEE100")
            .version("1")
            .volume(1)
            .build();

        return List.of(fee1, fee2);
    }

    public static String generateUniqueCCDCaseReferenceNumber() {

        Random rand = new Random();
        String ccdCaseNumber = String.format((Locale) null, //don't want any thousand separators
            "111122%04d%04d%02d",
            rand.nextInt(10000),
            rand.nextInt(10000),
            rand.nextInt(99));
        return ccdCaseNumber;
    }
}
