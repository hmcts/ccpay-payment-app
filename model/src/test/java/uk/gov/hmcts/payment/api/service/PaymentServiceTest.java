package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.payment.api.util.PaymentMethodUtil.CARD;

@RunWith(MockitoJUnitRunner.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentServiceImpl service;
    @Mock
    private Payment2Repository paymentRepository;
    @Mock
    private CardPaymentService<PaymentFeeLink, String> cardPaymentService;

    @Test
    public void shouldDelegateSearchToCardService() {
        // given
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now();
        List<PaymentFeeLink> paymentFeeLinks = Collections.emptyList();
        given(cardPaymentService.search(any(Date.class), any(Date.class), eq(CARD.name()), eq(null)))
            .willReturn(paymentFeeLinks);
        // when
        List<PaymentFeeLink> result = service.search(startDate, endDate, CARD, null);
        // then
        assertThat(result).isSameAs(paymentFeeLinks);
    }

    @Test
    public void shouldPassStartDateWithMidnightTimeForSearch() {
        // given
        LocalDate startDate = LocalDate.now();
        // when
        service.search(startDate, LocalDate.now(), CARD, null);
        // then
        Date fromDate = Date.from(LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        verify(cardPaymentService).search(eq(fromDate), any(Date.class), eq(CARD.name()), eq(null));
    }

    @Test
    public void shouldPassEndDateWithMidnightForSearch() {
        // given
        LocalDate startDate = LocalDate.now();
        // when
        service.search(startDate, LocalDate.now(), CARD, null);
        // then
        Date toDate = Date.from(LocalDate.now().atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());
        verify(cardPaymentService).search(any(Date.class), eq(toDate), eq(CARD.name()), eq(null));
    }

}
