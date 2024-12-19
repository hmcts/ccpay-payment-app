package uk.gov.hmcts.payment.api.model;

public interface DuplicateServiceRequestDto {
    String getFee_codes();
    Integer getPayment_link_id();
    String getCcd_case_number();
    String getEnterprise_service_name();
}
