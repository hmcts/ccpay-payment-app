package uk.gov.hmcts.payment.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.dto.CardPaymentFeeDto;

import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "createCardPaymentWith")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateCardPaymentResponse {
    private String status;

    private String reference;

    private String paymentGroupReference;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    private String externalReference;

    private LinksDto links;

    private List<CardPaymentFeeDto> fees;

    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    @JsonInclude(NON_NULL)
    @Getter
    public static class LinksDto {
        private LinkDto nextUrl;
        private LinkDto self;
        private LinkDto cancel;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(NON_NULL)
    @Getter
    public static class LinkDto {
        private String href;
        private String method;
    }
}
