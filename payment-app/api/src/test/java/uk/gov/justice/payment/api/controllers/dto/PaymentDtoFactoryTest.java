package uk.gov.justice.payment.api.controllers.dto;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.justice.payment.api.external.client.dto.Link;
import uk.gov.justice.payment.api.external.client.dto.Payment;
import uk.gov.justice.payment.api.external.client.dto.State;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.payment.api.controllers.dto.PaymentDto.paymentDtoWith;
import static uk.gov.justice.payment.api.external.client.dto.Payment.paymentWith;

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
        assertThat(new PaymentDtoFactory().toDto(paymentWith()
                .paymentId("123")
                .amount(500)
                .state(new State("status", false, "message", "code"))
                .description("description")
                .reference("reference")
                .createdDate("createdDate")
                .links(new Payment.Links(
                        null,
                        new Link("nextUrlType", ImmutableMap.of(), "nextUrlHref", "nextUrlMethod"),
                        null,
                        null,
                        null,
                        new Link("cancelType", ImmutableMap.of(), "cancelHref", "cancelMethod")
                ))
                .build())
        ).isEqualTo(paymentDtoWith()
                .paymentId("123")
                .amount(500)
                .state(new PaymentDto.StateDto("status", false, "message", "code"))
                .description("description")
                .reference("reference")
                .createdDate("createdDate")
                .links(new PaymentDto.LinksDto(
                        new PaymentDto.LinkDto("nextUrlHref", "nextUrlMethod"),
                        new PaymentDto.LinkDto("http://localhost/payments/123/cancel", "POST")
                ))
                .build());
    }

}