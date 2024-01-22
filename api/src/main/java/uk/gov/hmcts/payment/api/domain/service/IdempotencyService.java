package uk.gov.hmcts.payment.api.domain.service;

import uk.gov.hmcts.payment.api.model.IdempotencyKeys;

import java.util.List;
import java.util.Optional;

public interface IdempotencyService {
    Optional<IdempotencyKeys> findTheRecordByIdempotencyKey(String idempotencyKeyToCheck);

    List<IdempotencyKeys> findTheRecordByRequestHashcode(Integer requestHashcode);

    List<IdempotencyKeys> filterRecordsWithAcceptableLiberataHttpResponse(List<IdempotencyKeys> idempotencyKeysList);
}
