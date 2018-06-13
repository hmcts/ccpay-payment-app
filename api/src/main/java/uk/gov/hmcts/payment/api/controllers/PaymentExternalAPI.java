package uk.gov.hmcts.payment.api.controllers;

import java.lang.annotation.*;

/**
 * Annotation for marking apis to be categorise as externally accessible (eg : api gateway)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaymentExternalAPI {
}
