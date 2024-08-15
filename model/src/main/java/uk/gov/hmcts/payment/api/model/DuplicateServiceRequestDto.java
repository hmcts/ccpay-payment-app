package uk.gov.hmcts.payment.api.model;

import lombok.*;

import java.text.SimpleDateFormat;
import java.util.StringJoiner;
import java.util.TimeZone;

public interface DuplicateServiceRequestDto {
        String getCcd_case_number();
        Integer getCount();



}
