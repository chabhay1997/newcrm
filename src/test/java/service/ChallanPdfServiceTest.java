package service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ChallanPdfServiceTest {
    @Test
    void convertsAmountsUsingIndianNumbering() {
        assertThat(ChallanPdfService.numberToWords(BigDecimal.ZERO)).isEqualTo("ZERO");
        assertThat(ChallanPdfService.numberToWords(new BigDecimal("500.00"))).isEqualTo("FIVE HUNDRED");
        assertThat(ChallanPdfService.numberToWords(new BigDecimal("12345678")))
                .isEqualTo("ONE CRORE TWENTY THREE LAKH FORTY FIVE THOUSAND SIX HUNDRED SEVENTY EIGHT");
    }
}
