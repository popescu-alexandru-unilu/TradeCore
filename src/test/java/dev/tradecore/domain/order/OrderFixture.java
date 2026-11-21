package dev.tradecore.domain.order;
import java.math.BigDecimal;
public final class OrderFixture{
private OrderFixture(){}
public static Order validBuyOrder(){
    return Order.createOrder(
        1L,                        // Order ID
            10L,                       // Account ID
            5L,                        // Instrument ID
            OrderSide.BUY,
            BigDecimal.TEN,            // Quantity
            BigDecimal.valueOf(200)  
    );
}
public static Order buyOrder(BigDecimal quantity){
    return Order.createOrder(
         1L,                        // Order ID
            10L,                       // Account ID
            5L,                        // Instrument ID
            OrderSide.BUY,
            quantity,            // Quantity
            BigDecimal.valueOf(200)
           
    );
}

}