package uk.gov.hmcts.payment.functional.ccd;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import lombok.Data;
import uk.gov.hmcts.payment.functional.ccd.request.IacDraftCaseCreateRequest;

public interface CcdApi {

    @RequestLine("GET /caseworkers/{userId}/jurisdictions/IA/case-types/Asylum/event-triggers/startAppeal/token")
    @Headers({"Content-Type: application/json", "Authorization: {accessToken}", "ServiceAuthorization: {s2sToken}"})
    iacDraftCaseCreateTokenResponse getIacDraftCaseCreateToken(@Param("userId") String userId,
                                                                   @Param("accessToken") String accessToken,
                                                                   @Param("s2sToken") String s2sToken);

    @RequestLine("POST /case-types/Asylum/cases")
    @Headers({"Content-Type: application/json", "Authorization: {accessToken}", "ServiceAuthorization: {s2sToken}", "experimental: true"})
    IacCreateDraftCaseResponse createIacDraftCase(@Param("accessToken") String accessToken,
                                                          @Param("s2sToken") String s2sToken,
                                                          IacDraftCaseCreateRequest iacDraftCaseCreateRequest);

    @Data
    class iacDraftCaseCreateTokenResponse {
        @JsonProperty("token")
        private String token;
    }

    @Data
    class IacCreateDraftCaseResponse {
        @JsonProperty("id")
        private String id;
    }
}
