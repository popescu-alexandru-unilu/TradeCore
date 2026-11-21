package dev.tradecore.domain.order;
import java.math.BigDecimal;
public final class Order{
    private final Long id;
    private final Long accountId;
    private final Long instrumentId;

    private final OrderSide side;
    private  OrderStatus status;

    private final BigDecimal quantity;
    private  BigDecimal filledQuantity;
    private final BigDecimal limitPrice;
    private Order( Long id, Long accountId, Long instrumentId,OrderSide side,BigDecimal quantity, BigDecimal limitPrice){
            validatePositiveAmount(quantity);
            validatePositiveAmount(limitPrice);
            validatePositiveId(id);
            validatePositiveId(accountId);
            validatePositiveId(instrumentId);
            if(side == null)throw new IllegalArgumentException("Order side is required");
            this.id = id;
            this.accountId =accountId;
            this.instrumentId = instrumentId;
            this.side =side; 
            this.quantity = quantity;
            this.limitPrice=limitPrice;
            this.status = OrderStatus.PENDING;
            this.filledQuantity = BigDecimal.ZERO;
    }
    public static Order createOrder(Long id, Long accountId, Long instrumentId,OrderSide side,BigDecimal quantity, BigDecimal limitPrice){
        return new Order(id,accountId,instrumentId,side,quantity,limitPrice);
    }
    private void validatePositiveAmount(BigDecimal quantity){
     if(quantity == null ||quantity.compareTo(BigDecimal.ZERO) <= 0 ){throw new IllegalArgumentException("Amount should not be 0 or null");}
}
private void validatePositiveId(Long id){
     if(id == null ||id <= 0 ){throw new IllegalArgumentException("Invalid order id!");}
}
public BigDecimal getRemainingQuantity(){
    return quantity.subtract(filledQuantity);
}
public void applyFill(BigDecimal amount){
    validatePositiveAmount(amount);
  if(status == OrderStatus.CANCELLED || status == OrderStatus.FILLED)throw new IllegalStateException("Cannot execute a terminal order");
    if(amount.compareTo(getRemainingQuantity())> 0){throw new IllegalArgumentException(" Amounts exceeding remaining quantity are rejected");}
    BigDecimal newFill = filledQuantity.add(amount);
    OrderStatus newStatus = (newFill.compareTo(quantity) == 0)? OrderStatus.FILLED :OrderStatus.PARTIALLY_FILLED;
       
        if(!status.canTransitionTo(newStatus))throw new IllegalStateException("Invalid order transition");
        filledQuantity = newFill;
        status = newStatus;

        

} 
public void cancel(){
   if(!status.canTransitionTo(OrderStatus.CANCELLED))throw new IllegalStateException("Cannot execute a terminal order");
   status=OrderStatus.CANCELLED;
}
public OrderStatus getStatus(){return status;}
public BigDecimal getFilledQuantity(){return filledQuantity;}
}