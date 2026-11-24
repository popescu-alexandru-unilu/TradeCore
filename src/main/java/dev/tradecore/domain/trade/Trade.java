package dev.tradecore.domain.trade;
import java.math.BigDecimal;
import java.time.Instant;
public final class Trade {
private final Long tradeId;
private final Long buyOrderId;
private final Long sellOrderId;
private final BigDecimal quantity;
private final BigDecimal executionPrice;
private final Instant executedAt;
private Trade(Long tradeId, Long buyOrderId, Long sellOrderId, BigDecimal quantity, BigDecimal executionPrice, Instant executedAt){
validateId(tradeId);
validateId(buyOrderId);
validateId(sellOrderId);
validateAmounts(quantity);
validateAmounts(executionPrice);
if(executedAt == null)throw new IllegalArgumentException("Invalid execution time ");
this.tradeId =tradeId;
this.buyOrderId = buyOrderId;
this.sellOrderId = sellOrderId;
this.quantity = quantity;
this.executionPrice =executionPrice;
this.executedAt = executedAt;
}
public static Trade createTrade(Long tradeId, Long buyOrderId, Long sellOrderId, BigDecimal quantity, BigDecimal executionPrice, Instant executedAt){
    return new Trade(tradeId, buyOrderId, sellOrderId, quantity, executionPrice,executedAt);
}
private static void validateId(Long id){
 if(id == null ||id <= 0 ){throw new IllegalArgumentException("Invalid id record!");}
}
private static void validateAmounts(BigDecimal val ){
     if(val == null ||val.compareTo(BigDecimal.ZERO) <= 0 )throw new IllegalArgumentException("Amount must be positive");
}

}