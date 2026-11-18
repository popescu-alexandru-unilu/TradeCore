package dev.tradecore.domain.account;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTest{
 @Test
 void shouldReturnTheCorrectAvailableBalance(){
    Account acc = Account.createAccount(BigDecimal.valueOf(1000));
    acc.reserveFunds(BigDecimal.valueOf(200));
    assertEquals(BigDecimal.valueOf(800),acc.getAvailableBalance());
 }
 @Test
 void shouldRejectReservationWhenFundsAreInsufficient(){
     Account acc = Account.createAccount(BigDecimal.valueOf(1000));
    assertThrows(IllegalArgumentException.class,()->acc.reserveFunds(BigDecimal.valueOf(1200)) );
    assertEquals(BigDecimal.valueOf(1000),acc.getAvailableBalance());
 }
 @Test
 void shouldHandleMultipleReservations(){
     Account acc = Account.createAccount(BigDecimal.valueOf(1000));
      acc.reserveFunds(BigDecimal.valueOf(300));
       acc.reserveFunds(BigDecimal.valueOf(400));
       assertEquals(BigDecimal.valueOf(300),acc.getAvailableBalance());

 }
 @Test
 void shouldRejectInvalidReservationAmounts(){
     Account acc = Account.createAccount(BigDecimal.valueOf(1000));
    assertThrows(IllegalArgumentException.class,()-> acc.reserveFunds(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, ()-> acc.reserveFunds(BigDecimal.valueOf(-100)));
    assertEquals(BigDecimal.valueOf(1000),acc.getAvailableBalance());
 }
 @Test
 void shouldNotRejectNull(){
     Account acc = Account.createAccount(BigDecimal.valueOf(1000));
assertThrows(IllegalArgumentException.class,()->acc.reserveFunds(null));
 }
 @Test
 void shouldReleaseAllReservedFunds(){
     Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(300));
    acc.releaseFunds(BigDecimal.valueOf(300));
     assertEquals(BigDecimal.valueOf(1000),acc.getAvailableBalance());
 }
 @Test
 void shouldRejectReleaseExceedingReservedFunds(){
    Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(300));
    
     assertThrows(IllegalArgumentException.class, ()->  acc.releaseFunds(BigDecimal.valueOf(400)));
      assertEquals(BigDecimal.valueOf(700),acc.getAvailableBalance());
 }
 @Test
 void shouldExecuteReservedFunds(){
    Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(300));
     acc.executeReservedFunds(BigDecimal.valueOf(200));
     assertEquals(BigDecimal.valueOf(700),acc.getAvailableBalance());
     assertEquals(BigDecimal.valueOf(800),acc.getBalance());
     assertEquals(BigDecimal.valueOf(100),acc.getReserved());
 }
 @Test
 void shouldRejectExecutionExceedingReservedFunds(){
    Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(300));
    
     assertThrows(IllegalArgumentException.class,()-> acc.executeReservedFunds(BigDecimal.valueOf(400)));
      assertEquals(BigDecimal.valueOf(700),acc.getAvailableBalance());
 }
 @Test
 void shouldPreserveAccountStateAfterFailedExecution(){
    Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(300));
     assertThrows(IllegalArgumentException.class,()-> acc.executeReservedFunds(BigDecimal.valueOf(400)));
     assertEquals(BigDecimal.valueOf(700),acc.getAvailableBalance());
     assertEquals(BigDecimal.valueOf(1000),acc.getBalance());
     assertEquals(BigDecimal.valueOf(300),acc.getReserved());

 }
 @Test
 void shouldExecuteAllReservedFunds(){
     Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(300));
     acc.executeReservedFunds(BigDecimal.valueOf(300));
     assertEquals(BigDecimal.valueOf(700),acc.getAvailableBalance());
     assertEquals(BigDecimal.valueOf(700),acc.getBalance());
     assertEquals(BigDecimal.ZERO,acc.getReserved());

 }
 @Test
 void shouldReleasePartialReservedFunds(){
      Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(400));
     acc.releaseFunds(BigDecimal.valueOf(150));
     assertEquals(BigDecimal.valueOf(750),acc.getAvailableBalance());
     assertEquals(BigDecimal.valueOf(1000),acc.getBalance());
     assertEquals(BigDecimal.valueOf(250),acc.getReserved());
 }
 @Test
 void shouldMaintainCorrectBalancesAcrossOperations(){
 Account acc = Account.createAccount(BigDecimal.valueOf(1000));
     acc.reserveFunds(BigDecimal.valueOf(400));
     acc.releaseFunds(BigDecimal.valueOf(150));
     acc.executeReservedFunds(BigDecimal.valueOf(250));
     assertEquals(BigDecimal.valueOf(750),acc.getAvailableBalance());
     assertEquals(BigDecimal.valueOf(750),acc.getBalance());
     assertEquals(BigDecimal.ZERO,acc.getReserved());

 }
}