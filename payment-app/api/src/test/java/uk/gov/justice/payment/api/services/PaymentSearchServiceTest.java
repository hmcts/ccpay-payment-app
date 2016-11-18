package uk.gov.justice.payment.api.services;

import org.junit.Test;
import uk.gov.justice.payment.api.exceptions.ApplicationException;
import uk.gov.justice.payment.api.exceptions.PaymentNotFoundException;
import uk.gov.justice.payment.api.model.PaymentDetails;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;

public class PaymentSearchServiceTest {

    @Test(expected = PaymentNotFoundException.class)
    public void findOneShouldThrowExceptionIfNoResultsFound() {
        searchServiceReturning(emptyList()).findOne("any", null);
    }

    @Test(expected = ApplicationException.class)
    public void findOneShouldThrowExceptionIfMoreThanOneResultFound() {
        searchServiceReturning(asList(new PaymentDetails(), new PaymentDetails())).findOne("any", null);
    }

    @Test
    public void findOneShouldReturnInCaseOnlyOneResultFound() {
        assertThat(searchServiceReturning(singletonList(new PaymentDetails())).findOne("any", null)).isEqualTo(new PaymentDetails());
    }

    private PaymentSearchService searchServiceReturning(List<PaymentDetails> list) {
        return (serviceId, searchCriteria) -> list;
    }
}