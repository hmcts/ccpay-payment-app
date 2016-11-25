package uk.gov.justice.payment.api.controllers.dto;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.justice.payment.api.contract.PaymentDto;
import uk.gov.justice.payment.api.controllers.PaymentDtoFactory;
import uk.gov.justice.payment.api.model.PaymentDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.payment.api.contract.PaymentDto.paymentDtoWith;

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
        assertThat(new PaymentDtoFactory().toDto(PaymentDetails.paymentDetailsWith()
                .paymentId("123")
                .amount(500)
                .status("status")
                .finished(false)
                .description("description")
                .applicationReference("application_reference")
                .paymentReference("payment_reference")
                .createdDate("createdDate")
                .cancelUrl("http://cancel_url")
                .nextUrl("http://next_url")
                .build())
        ).isEqualTo(paymentDtoWith()
                .paymentId("123")
                .amount(500)
                .state(new PaymentDto.StateDto("status", false))
                .description("description")
                .applicationReference("application_reference")
                .paymentReference("payment_reference")
                .createdDate("createdDate")
                .links(new PaymentDto.LinksDto(
                        new PaymentDto.LinkDto("http://next_url", "GET"),
                        new PaymentDto.LinkDto("http://localhost/payments/123/cancel", "POST")
                ))
                .build());
    }

}