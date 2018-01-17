package uk.gov.hmcts.payment.api.controllers;

import java.math.BigDecimal;

public class TestAmount {

    public static void main(String[] args) {
        BigDecimal amt = new BigDecimal("100.99");
        System.out.println("Amount in pence : " + amt.movePointRight(2).intValue());
    }
}

