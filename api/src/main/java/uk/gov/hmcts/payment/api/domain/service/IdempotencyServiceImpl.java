package uk.gov.hmcts.payment.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.IdempotencyKeysRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    @Autowired
    private IdempotencyKeysRepository idempotencyKeysRepository;

    private static final int HTTP_CODE_ALLOWABLE_RETRIES[] = { 504, 412, 402 };

    @Override
    public Optional<IdempotencyKeys> findTheRecordByIdempotencyKey(String idempotencyKeyToCheck) {
        return idempotencyKeysRepository.findByIdempotencyKey(idempotencyKeyToCheck);
    }

    @Override
    public List<IdempotencyKeys> findTheRecordByRequestHashcode(Integer requestHashcode) {
        List<IdempotencyKeys> idempotencyKeyRows =
            idempotencyKeysRepository.findByRequestHashcode(requestHashcode);

        // Return all Idempotency records which are either still pending (payment in progress)
        // or completed and do not have one of the known valid http responses which can allow a retry of the payment.
        idempotencyKeyRows = idempotencyKeyRows.stream().filter(keys ->
            (!keys.getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed) ||
                (keys.getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed) &&
                 !Arrays.stream(HTTP_CODE_ALLOWABLE_RETRIES).anyMatch(keys.getResponseCode()::equals))
                )).collect(Collectors.toList());

        return idempotencyKeyRows;
    }
}
