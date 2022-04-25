package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.UserIdentityDataDto;
import uk.gov.hmcts.payment.api.dto.idam.IdamUserIdResponse;

public interface IdamService {

    IdamUserIdResponse getUserId(MultiValueMap<String, String> headers);

    UserIdentityDataDto getUserIdentityData(MultiValueMap<String, String> headers, String uid);

    String getUserDetails(MultiValueMap<String, String> headers);

}
