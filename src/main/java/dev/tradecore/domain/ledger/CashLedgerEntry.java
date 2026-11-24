package dev.tradecore.domain.ledger;
import java.math.BigDecimal;
import java.time.Instant;
public final class CashLedgerEntry{

private final Long entryId;
private final Long accountId;
private final Long tradeId;
private final BigDecimal amount;
private final String currency;
private final LedgerEntryType type;
private final Instant recordedAt;
private  CashLedgerEntry(Long entryId, Long accountId, Long tradeId, BigDecimal amount, String currency, LedgerEntryType type, Instant recordedAt){
    validateAmount(type,amount);
    validateId(entryId);
    validateId(accountId);
    
    if(currency == null || currency.isBlank())throw new IllegalArgumentException(" Currency must be exist");
    if(recordedAt == null)throw new IllegalArgumentException("Time must exist");
    if(type.requiresTrade()){
        validateId(tradeId);
        
    }else if (tradeId != null){throw new IllegalArgumentException("Non-trade entries cannot reference a trade");}
    this.entryId = entryId;
    this.accountId = accountId;
    this.tradeId = tradeId;
    this.amount =amount;
    this.currency = currency;
    this.type = type;
    this.recordedAt =recordedAt;
    
};
public static CashLedgerEntry createLedgerEntry(Long entryId, Long accountId, Long tradeId, BigDecimal amount, String currency, LedgerEntryType type, Instant recordedAt){
    return new CashLedgerEntry(entryId,accountId,tradeId,amount,currency,type,recordedAt);
}
private static void validateAmount(LedgerEntryType type,BigDecimal amount){
    if(amount == null || type == null)throw new IllegalArgumentException("No null values");
   int sign = amount.compareTo(BigDecimal.ZERO);
   if(sign == 0){throw new IllegalArgumentException("Amount cannot be zero");}
   if(type.increasesCash() && sign < 0){throw new IllegalArgumentException("Only positive amounts allowed");}
   if(type.decreasesCash() && sign > 0)throw new IllegalArgumentException("Only negative amounts allowed");
}
private static void validateId(Long id){
 if(id == null ||id <= 0 ){throw new IllegalArgumentException("Invalid id record!");}
}
public BigDecimal getAmount(){
    return amount;
}
public Long getEntryId(){
    return entryId;
}
public LedgerEntryType getType(){
    return type;
}
public Instant getRecordedAt(){
    return recordedAt;
}
}