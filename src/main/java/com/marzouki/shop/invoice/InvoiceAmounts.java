package com.marzouki.shop.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shop prices include VAT, so the invoice shows how much of the total is VAT at the invoice's rate. */
public record InvoiceAmounts(BigDecimal total, BigDecimal net, BigDecimal vat) {

    public static InvoiceAmounts of(BigDecimal total, BigDecimal vatPercent) {
        BigDecimal divisor = BigDecimal.ONE.add(vatPercent.movePointLeft(2));
        BigDecimal net = total.divide(divisor, 2, RoundingMode.HALF_UP);
        return new InvoiceAmounts(total, net, total.subtract(net));
    }
}