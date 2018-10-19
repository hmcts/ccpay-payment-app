package uk.gov.hmcts.payment.api.service;

import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.payment.api.util.PaymentMethodType.CARD;

@RunWith(MockitoJUnitRunner.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentServiceImpl service;

    @Mock
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Test
    public void shouldDelegateSearchToCardPaymentType() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();
        List<PaymentFeeLink> paymentFeeLinks = Collections.emptyList();
        given(delegatingPaymentService.search(any(Date.class), any(Date.class), eq(CARD.getType()), eq(null), eq(null), eq(null)))
            .willReturn(paymentFeeLinks);
        // when
        List<PaymentFeeLink> result = service.search(startDate, endDate, CARD.getType(), null, null, null);
        // then
        assertThat(result).isSameAs(paymentFeeLinks);
    }

    @Test
    public void shouldPassStartDateWithMidnightTimeForSearch() {
        // given
        LocalDateTime startDate = LocalDateTime.now();
        // when
        service.search(startDate, LocalDateTime.now(), CARD.getType(), null, null, null);
        // then
        Date fromDate = LocalDateTime.now().toDate();
        verify(delegatingPaymentService).search(any(startDate.toDate().getClass()), any(LocalDateTime.now().toDate().getClass()), eq(CARD.getType()), eq(null), eq(null), eq(null));
    }

    @Test
    public void shouldPassEndDateWithMidnightForSearch() {
        // given
        LocalDateTime startDate = LocalDateTime.now();
        // when
        service.search(startDate, LocalDateTime.now(), CARD.getType(), null, null, null);
        // then
        Date toDate = LocalDateTime.now().plusDays(1).minusSeconds(1).toDate(); //Date.from(LocalDateTime.now().atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());
        verify(delegatingPaymentService).search(any(startDate.toDate().getClass()), any(toDate.getClass()), eq(CARD.getType()), eq(null), eq(null), eq(null));
    }

    @Test
    public void shouldDelegateSearchToCMCService() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();
        List<PaymentFeeLink> paymentFeeLinks = Collections.emptyList();
        given(delegatingPaymentService.search(any(Date.class), any(Date.class), eq(CARD.getType()), eq("cmc"), eq(null), eq(null)))
            .willReturn(paymentFeeLinks);
        // when
        List<PaymentFeeLink> result = service.search(startDate, endDate, CARD.getType(), "cmc", null, null);
        // then
        assertThat(result).isSameAs(paymentFeeLinks);
    }

}
