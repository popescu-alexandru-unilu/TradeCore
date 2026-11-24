package dev.tradecore.domain.ledger;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class CashLedgerEntryTest{
@Test
void shouldCreateValidDepositEntry(){
    CashLedgerEntry entry = CashLedgerEntry.createLedgerEntry(1L,
    10L,
    null,
    BigDecimal.valueOf(1000),
    "GBP",
    LedgerEntryType.DEPOSIT,
    Instant.parse("2026-11-23T10:00:00Z"));
    assertEquals(BigDecimal.valueOf(1000),entry.getAmount());
    assertEquals(LedgerEntryType.DEPOSIT,entry.getType());
}
@Test 
void shouldRejectPositiveTradeBuyAmount(){
    
    assertThrows(IllegalArgumentException.class,()->  CashLedgerEntry.createLedgerEntry(1L,
    10L,
    20L,
    BigDecimal.valueOf(500),
    "GBP",
    LedgerEntryType.TRADE_BUY,
    Instant.parse("2026-11-23T10:00:00Z")));
}
}



