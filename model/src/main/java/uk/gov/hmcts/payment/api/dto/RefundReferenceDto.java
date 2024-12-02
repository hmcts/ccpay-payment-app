package uk.gov.hmcts.payment.api.dto;

import lombok.*;

import javax.validation.constraints.NotEmpty;

@Builder(builderMethodName = "refundReferenceDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RefundReferenceDto {

    @NotEmpty(message = "Remission reference can not be empty")
    private String remissionReference;

}
