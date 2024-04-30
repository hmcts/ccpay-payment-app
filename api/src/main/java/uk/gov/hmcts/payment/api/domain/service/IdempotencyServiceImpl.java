package uk.gov.hmcts.payment.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.IdempotencyKeysRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    @Autowired
    private IdempotencyKeysRepository idempotencyKeysRepository;

    private static final int HTTP_CODE_ALLOWABLE_RETRIES[] = { 504, 500, 412, 402 };

    @Override
    public Optional<IdempotencyKeys> findTheRecordByIdempotencyKey(String idempotencyKeyToCheck) {
        return idempotencyKeysRepository.findByIdempotencyKey(idempotencyKeyToCheck);
    }

    @Override
    public List<IdempotencyKeys> findTheRecordByRequestHashcode(Integer requestHashcode) {
        return idempotencyKeysRepository.findByRequestHashcode(requestHashcode);
    }

    @Override
    public List<IdempotencyKeys> filterRecordsWithAcceptableLiberataHttpResponse(List<IdempotencyKeys> idempotencyKeysList) {
        // Return all Idempotency records which are either still pending (payment in progress)
        // or completed and do not have one of the known valid http responses which can allow a retry of the payment.
        List<IdempotencyKeys> idempotencyKeys = idempotencyKeysList.stream().filter(keys ->
            (!keys.getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed) ||
                (keys.getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed) &&
                    !Arrays.stream(HTTP_CODE_ALLOWABLE_RETRIES).anyMatch(keys.getResponseCode()::equals))
            )).collect(Collectors.toList());

        return idempotencyKeys;
    }
}
