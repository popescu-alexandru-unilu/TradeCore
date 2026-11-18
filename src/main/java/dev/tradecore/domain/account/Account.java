package dev.tradecore.domain.account;
import java.math.BigDecimal;
public final class Account{
    private BigDecimal reserved;
    private BigDecimal balance;
    private Account (BigDecimal initialBalance){
        if(initialBalance == null){throw new IllegalArgumentException("The initialBalance cannot be null");}
        if(initialBalance.compareTo(BigDecimal.ZERO) < 0){throw new IllegalArgumentException("The initialBalance cannot be smaller than 0");}
        this.reserved= BigDecimal.ZERO;
        this.balance =initialBalance;
     

    }
    public static Account createAccount(BigDecimal initialBalance){
        return new Account(initialBalance);
        }
    public BigDecimal getAvailableBalance(){
        return balance.subtract(reserved);
    }
    public void reserveFunds(BigDecimal amount){
        validatePositiveAmount(amount);
        if(amount.compareTo(getAvailableBalance()) > 0){throw new IllegalArgumentException("Insufficient available funds");}
        reserved = reserved.add(amount);
        
    }
    public void releaseFunds(BigDecimal amount){
        validatePositiveAmount(amount);
        if(amount.compareTo(reserved) > 0){throw new IllegalArgumentException("Insufficient available funds");}
        reserved =reserved.subtract(amount);
    }
    public void executeReservedFunds(BigDecimal amount){
        validatePositiveAmount(amount);
        if(amount.compareTo(reserved)>0){throw new IllegalArgumentException("Insufficient reserved  funds");}
        BigDecimal newReserved = reserved.subtract(amount);
        BigDecimal newBalance = balance.subtract(amount);
        reserved= newReserved;
        balance = newBalance;
    }
private void validatePositiveAmount(BigDecimal amount){
     if(amount == null ||amount.compareTo(BigDecimal.ZERO) <= 0 ){throw new IllegalArgumentException("Amount should not be 0 or null");}
}
public BigDecimal getBalance(){
    return balance;
}
public BigDecimal getReserved(){
    return reserved;
}
}