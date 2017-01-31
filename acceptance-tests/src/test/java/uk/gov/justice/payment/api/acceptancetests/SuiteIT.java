package uk.gov.justice.payment.api.acceptancetests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({

        PaymentsHappyPathTest.class,
        PaymentsMandatoryValuesTest.class,
        HealthCheck.class,
        PaymentsKeysMissingValidationTest.class


})

public class SuiteIT {

}
