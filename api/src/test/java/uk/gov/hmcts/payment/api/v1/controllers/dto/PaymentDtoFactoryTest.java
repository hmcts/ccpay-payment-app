package uk.gov.hmcts.payment.api.v1.controllers.dto;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.payment.api.dto.PaymentOldDto;
import uk.gov.hmcts.payment.api.v1.controllers.PaymentDtoFactory;
import uk.gov.hmcts.payment.api.v1.model.PaymentOld;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.dto.PaymentOldDto.paymentDtoWith;

public class PaymentDtoFactoryTest {

    @Before
    public void setup() {
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
        when(httpServletRequestMock.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(httpServletRequestMock.getRequestURI()).thenReturn("http://localhost");
        when(httpServletRequestMock.getContextPath()).thenReturn(StringUtils.EMPTY);
        when(httpServletRequestMock.getServletPath()).thenReturn(StringUtils.EMPTY);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequestMock));
    }

    @Test
    public void convertsToDto() {
        Assertions.assertThat(new PaymentDtoFactory().toDto(PaymentOld.paymentWith()
            .userId("123")
            .id(456)
            .govPayId("govPayId")
            .amount(500)
            .status("status")
            .finished(false)
            .description("description")
            .reference("reference")
            .dateCreated(new Date(123456789))
            .cancelUrl("http://cancel_url")
            .nextUrl("http://next_url")
            .build())
        ).isEqualTo(paymentDtoWith()
            .id("456")
            .amount(500)
            .state(new PaymentOldDto.StateDto("status", false))
            .description("description")
            .reference("reference")
            .dateCreated(new Date(123456789))
            .links(new PaymentOldDto.LinksDto(
                new PaymentOldDto.LinkDto("http://next_url", "GET"),
                new PaymentOldDto.LinkDto("/users/123/payments/456/cancel", "POST")
            ))
            .build());
    }

}
