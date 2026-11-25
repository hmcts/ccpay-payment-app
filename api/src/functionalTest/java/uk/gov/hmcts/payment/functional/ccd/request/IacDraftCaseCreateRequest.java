package uk.gov.hmcts.payment.functional.ccd.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IacDraftCaseCreateRequest {

    @Builder.Default
    @JsonProperty("data")
    private Data data = new Data();

    @Builder.Default
    @JsonProperty("event")
    private Event event = new Event();

    @JsonProperty("event_token")
    private String eventToken;

    @Builder.Default
    @JsonProperty("ignore_warning")
    private Boolean ignoreWarning = false;

    @Builder.Default
    @JsonProperty("draft_id")
    private String draftId = null;

    // ------------------------------------------------------------------ //

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        @Builder.Default
        private List<Object> searchParties = new ArrayList<>();

        @Builder.Default
        private String isCaseUsingLocationRefData = null;

        @Builder.Default
        private Checklist checklist = new Checklist();

        @Builder.Default
        private String isOutOfCountryEnabled = "Yes";

        @Builder.Default
        private String appellantInUk = "Yes";

        @Builder.Default
        private String homeOfficeReferenceNumber = "656566566";

        @Builder.Default
        private String homeOfficeDecisionDate = "2026-11-10";

        @Builder.Default
        private String appealType = "refusalOfHumanRights";

        @Builder.Default
        private AppealGrounds appealGroundsDecisionHumanRightsRefusal =
            new AppealGrounds(new ArrayList<>(List.of("humanRightsRefusal")));

        @Builder.Default
        private String appellantTitle = "mr";

        @Builder.Default
        private String appellantGivenNames = "test firstname";

        @Builder.Default
        private String appellantFamilyName = "test familyname";

        @Builder.Default
        private String appellantDateOfBirth = "2000-05-05";

        @Builder.Default
        private String appellantStateless = "isStateless";

        @Builder.Default
        private String appellantHasFixedAddress = "No";

        @Builder.Default
        private String contactPreference = "wantsEmail";

        @Builder.Default
        private String email = "test@test.com";

        @Builder.Default
        private String hasSponsor = "No";

        @Builder.Default
        private String deportationOrderOptions = "No";

        @Builder.Default
        private String hasNewMatters = "No";

        @Builder.Default
        private String hasOtherAppeals = "No";

        @Builder.Default
        private String hearingTypeResult = "No";

        @Builder.Default
        private String legalRepCompany = "ia-legal-rep-org456";

        @Builder.Default
        private String legalRepName = "test";

        @Builder.Default
        private String legalRepFamilyName = "test";

        @Builder.Default
        private String legalRepMobilePhoneNumber = "01753971698";

        @Builder.Default
        private String legalRepReferenceNumber = "test";

        @Builder.Default
        private String isFeePaymentEnabled = "Yes";

        @Builder.Default
        private String isRemissionsEnabled = "Yes";

        @Builder.Default
        private String decisionHearingFeeOption = "decisionWithHearing";

        @Builder.Default
        private String remissionType = "noRemission";

        @Builder.Default
        private String feeWithHearing = null;

        @Builder.Default
        private String feeWithoutHearing = null;
    }

    // ------------------------------------------------------------------ //

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Checklist {

        @Builder.Default
        private List<String> checklist1 = new ArrayList<>();

        @Builder.Default
        private List<String> checklist2 = new ArrayList<>(List.of("isNotDetained"));

        @Builder.Default
        private List<String> checklist3 = new ArrayList<>();

        @Builder.Default
        private List<String> checklist4 = new ArrayList<>();

        @Builder.Default
        private List<String> checklist5 = new ArrayList<>();

        @Builder.Default
        private List<String> checklist6 = new ArrayList<>();

        @Builder.Default
        private List<String> checklist7 = new ArrayList<>();
    }

    // ------------------------------------------------------------------ //

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppealGrounds {

        @Builder.Default
        private List<String> values = new ArrayList<>();
    }

    // ------------------------------------------------------------------ //

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {

        @Builder.Default
        private String id = "startAppeal";

        @Builder.Default
        private String summary = "";

        @Builder.Default
        private String description = "";
    }
}

