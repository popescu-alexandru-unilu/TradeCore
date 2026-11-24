package dev.tradecore.domain.ledger;

public enum LedgerEntryType{
DEPOSIT,
WITHDRAWAL,
TRADE_BUY,
TRADE_SELL;
public boolean requiresTrade(){
    return (this == TRADE_BUY|| this == TRADE_SELL);
}
public boolean increasesCash(){
    return (this == DEPOSIT || this == TRADE_SELL);
}
public boolean decreasesCash(){
    return (this == WITHDRAWAL || this == TRADE_BUY);
}
}