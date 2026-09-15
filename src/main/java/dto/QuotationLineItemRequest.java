package dto;

import java.math.BigDecimal;

public record QuotationLineItemRequest(String description, String hsnCode, Integer quantity,
                                       BigDecimal actualPrice, BigDecimal price) {
}
