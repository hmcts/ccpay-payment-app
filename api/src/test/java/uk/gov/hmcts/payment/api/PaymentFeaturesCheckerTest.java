package uk.gov.hmcts.payment.api;

import com.google.common.collect.Lists;
import org.ff4j.FF4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class PaymentFeaturesCheckerTest {

    private static final String FINREM = "finrem";

    @Mock private ServiceIdSupplier serviceIdSupplier;
    @Mock private FF4j ff4j;

    private PaymentFeaturesChecker checker;

    @Test
    public void shouldReturnTrueWhenFeatureEnabledAndServiceIdFinRem() {
        //given
        checker = new PaymentFeaturesChecker(serviceIdSupplier, ff4j, Lists.newArrayList(FINREM));
        given(ff4j.check("credit-account-payment-liberata-check")).willReturn(true);
        given(serviceIdSupplier.get()).willReturn(FINREM);
        // when & then
        assertThat(checker.isAccountStatusCheckRequired()).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenFeatureEnabledAndServiceIdIsNotFinRem() {
        //given
        checker = new PaymentFeaturesChecker(serviceIdSupplier, ff4j, Lists.newArrayList(FINREM));
        given(ff4j.check("credit-account-payment-liberata-check")).willReturn(true);
        given(serviceIdSupplier.get()).willReturn("cmc");
        // when & then
        assertThat(checker.isAccountStatusCheckRequired()).isFalse();
    }

    @Test
    public void shouldReturnFalseWhenFeatureIsDisabled() {
        //given
        checker = new PaymentFeaturesChecker(serviceIdSupplier, ff4j, Lists.newArrayList(FINREM));
        given(ff4j.check("credit-account-payment-liberata-check")).willReturn(false);
        // when & then
        assertThat(checker.isAccountStatusCheckRequired()).isFalse();
    }
}
