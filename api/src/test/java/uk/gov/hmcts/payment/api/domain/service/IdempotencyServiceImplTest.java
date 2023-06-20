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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        assertThat(response).isNotNull();
    }

    @Test
    public void findTheRecordByRequestHashcode() {

        IdempotencyKeys idempotencyKeys = IdempotencyKeys.idempotencyKeysWith().responseCode(HttpStatus.CONFLICT.value()).requestHashcode(-1644540583).idempotencyKey("").requestBody("").build();
        List<IdempotencyKeys> result = List.of(idempotencyKeys);

        when(idempotencyKeysRepository.findByRequestHashcode(any())).thenReturn(result);

        var response = idempotencyServiceImpl.findTheRecordByRequestHashcode(3333);
        assertThat(response).isNotNull();
    }
}
