package uk.gov.hmcts.payment.api.domain.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.IdempotencyKeysRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
public class IdempotencyServiceImplTest {

    @Mock
    private IdempotencyKeysRepository idempotencyKeysRepository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyServiceImpl;


    @Test
    public void findTheRecordByIdempotencyKey() {

        IdempotencyKeys idempotencyKeys = IdempotencyKeys.idempotencyKeysWith().responseCode(HttpStatus.CONFLICT.value()).requestHashcode(-1644540583).idempotencyKey("").requestBody("").build();

        when(idempotencyKeysRepository.findByIdempotencyKey(any())).thenReturn(Optional.of(idempotencyKeys));

        var response = idempotencyServiceImpl.findTheRecordByIdempotencyKey("test");
        assertNotNull(response);
    }

    @Test
    public void findTheRecordByRequestHashcode() {

        IdempotencyKeys idempotencyKeys = IdempotencyKeys.idempotencyKeysWith().responseCode(HttpStatus.CONFLICT.value()).
            requestHashcode(-1644540583).idempotencyKey("").requestBody("").responseStatus(IdempotencyKeys.ResponseStatusType.pending).build();

        List<IdempotencyKeys> result = List.of(idempotencyKeys);

        when(idempotencyKeysRepository.findByRequestHashcode(any())).thenReturn(result);

        List<IdempotencyKeys> response = idempotencyServiceImpl.findTheRecordByRequestHashcode(3333);
        assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    public void findTheRecordByRequestHashcodeWithAnEmptyList() {

        List<IdempotencyKeys> result = List.of();

        when(idempotencyKeysRepository.findByRequestHashcode(any())).thenReturn(result);

        List<IdempotencyKeys> response = idempotencyServiceImpl.findTheRecordByRequestHashcode(3333);
        assertNotNull(response);
        assertEquals(0, response.size());
    }

    @Test
    public void findTheRecordByRequestHashcodeWithFilteredResults() {

        IdempotencyKeys idempotencyKeys1 = IdempotencyKeys.idempotencyKeysWith().responseCode(HttpStatus.GATEWAY_TIMEOUT.value()).
            requestHashcode(-1644540583).idempotencyKey("idempKey01").requestBody("").responseStatus(IdempotencyKeys.ResponseStatusType.completed).build();

        IdempotencyKeys idempotencyKeys2 = IdempotencyKeys.idempotencyKeysWith().
            requestHashcode(-1644540583).idempotencyKey("idempKey02").requestBody("").responseStatus(IdempotencyKeys.ResponseStatusType.pending).build();

        IdempotencyKeys idempotencyKeys3 = IdempotencyKeys.idempotencyKeysWith().responseCode(HttpStatus.PAYMENT_REQUIRED.value()).
            requestHashcode(-1644540583).idempotencyKey("idempKey03").requestBody("").responseStatus(IdempotencyKeys.ResponseStatusType.completed).build();

        IdempotencyKeys idempotencyKeys4 = IdempotencyKeys.idempotencyKeysWith().responseCode(HttpStatus.CONFLICT.value()).
            requestHashcode(-1644540583).idempotencyKey("idempKey04").requestBody("").responseStatus(IdempotencyKeys.ResponseStatusType.completed).build();

        List<IdempotencyKeys> results = List.of(idempotencyKeys1, idempotencyKeys2, idempotencyKeys3, idempotencyKeys4);

        when(idempotencyKeysRepository.findByRequestHashcode(any())).thenReturn(results);

        List<IdempotencyKeys> response = idempotencyServiceImpl.findTheRecordByRequestHashcode(3333);
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("idempKey02", response.get(0).getIdempotencyKey());
        assertEquals("idempKey04", response.get(1).getIdempotencyKey());
    }
}
