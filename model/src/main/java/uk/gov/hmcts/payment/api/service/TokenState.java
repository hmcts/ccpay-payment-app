package uk.gov.hmcts.payment.api.service;

import java.time.Instant;

public record TokenState(String token, Instant expiresAtUtc) {}
