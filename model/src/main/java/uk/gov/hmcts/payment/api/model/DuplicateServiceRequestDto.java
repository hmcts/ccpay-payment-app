package uk.gov.hmcts.payment.api.model;

public interface DuplicateServiceRequestDto {
    String getCcd_case_number();

    Integer getCount();
}
