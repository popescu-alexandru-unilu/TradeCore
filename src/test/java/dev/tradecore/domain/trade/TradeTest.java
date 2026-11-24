package dev.tradecore.domain.trade;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class TradeTest{
@Test
void shouldCreateValidTrade(){
    Trade trade = Trade.createTrade(1L,
    10L,
    20L,
    BigDecimal.valueOf(5),
    BigDecimal.valueOf(200),
    Instant.parse("2026-11-23T10:00:00Z"));
    assertNotNull(trade);
}
@Test
void shouldRejectTradeWithNegativeQuantity(){
      
    assertThrows(IllegalArgumentException.class, ()->Trade.createTrade(1L,
    10L,
    20L,
    BigDecimal.valueOf(-5),
    BigDecimal.valueOf(200),
    Instant.parse("2026-11-23T10:00:00Z"))
    );
}
}