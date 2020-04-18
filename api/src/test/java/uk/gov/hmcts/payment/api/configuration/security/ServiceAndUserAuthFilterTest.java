package uk.gov.hmcts.payment.api.configuration.security;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.payment.api.configuration.SecurityUtils;
import uk.gov.hmcts.payment.api.configuration.security.authcheckerconfiguration.AuthCheckerConfiguration;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAndUserAuthFilterTest {

    @Mock
    private SecurityUtils securityUtils;

    private ServiceAndUserAuthFilter filter;


    private MockHttpServletRequest request;

    private HttpServletResponse response;

    private FilterChain filterChain;

    @Before
    public void setUp() {
        AuthCheckerConfiguration config = new AuthCheckerConfiguration();
        filter = new ServiceAndUserAuthFilter(config.userIdExtractor(),
            config.authorizedRolesExtractor(), securityUtils);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    public void shouldReturn200ResponseWhenUserIdAndRoleMatches() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(Arrays.asList("payments"))
            .build();

        request.setRequestURI("/citizens/user123/jurisdictions/AUTOTEST1/case-types");
        when(securityUtils.getUserInfo()).thenReturn(userInfo);
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldReturn403ResponseWhenUserIdIsNotSame() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("invalidUser")
            .roles(Lists.newArrayList("caseworker-autotest1"))
            .build();

        request.setRequestURI("/caseworkers/user123/jurisdictions/AUTOTEST1/case-types");
        when(securityUtils.getUserInfo()).thenReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void shouldReturn403ResponseWhenRoleNotMatches() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(Lists.newArrayList("caseworker-autotest123"))
            .build();

        request.setRequestURI("/caseworkers/user123/jurisdictions/AUTOTEST1/case-types");

        when(securityUtils.getUserInfo()).thenReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void shouldReturn403ResponseWhenCitizenPathRequestNotHavingValidRole() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(Lists.newArrayList("caseworker-autotest1"))
            .build();

        request.setRequestURI("/citizens/user123/jurisdictions/AUTOTEST1/case-types");

        when(securityUtils.getUserInfo()).thenReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
