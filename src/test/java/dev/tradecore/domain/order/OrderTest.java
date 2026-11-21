package dev.tradecore.domain.order;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class OrderTest{
    @Test
    void shouldCancelPartiallyFilledOrder(){
        Order order = OrderFixture.buyOrder(BigDecimal.valueOf(10));
        order.applyFill(BigDecimal.valueOf(4));
        order.cancel();
        assertEquals(OrderStatus.CANCELLED,order.getStatus());
        assertEquals(BigDecimal.valueOf(4),order.getFilledQuantity());
        assertEquals(BigDecimal.valueOf(6), order.getRemainingQuantity());
    }
    @Test
    void shouldRejectFillAfterCancellation(){
        Order order = OrderFixture.buyOrder(BigDecimal.valueOf(10));
        order.cancel();
        assertThrows(IllegalStateException.class, ()-> order.applyFill(BigDecimal.valueOf(3)));
      
    }
    @Test
    void shouldRejectFillAfterFullExecution(){
           Order order = OrderFixture.buyOrder(BigDecimal.valueOf(10));
             order.applyFill(BigDecimal.valueOf(10));
              
              assertThrows(IllegalStateException.class, ()-> order.applyFill(BigDecimal.valueOf(2)));
              assertEquals(OrderStatus.FILLED,order.getStatus());
              assertEquals(BigDecimal.valueOf(10),order.getFilledQuantity());

    }
    @Test
    void shouldRejectFillExceedingRemainingQuantity(){
        Order order = OrderFixture.buyOrder(BigDecimal.valueOf(10));
        order.applyFill(BigDecimal.valueOf(4));
        assertThrows(IllegalArgumentException.class,()-> order.applyFill(BigDecimal.valueOf(7)));
        assertEquals(BigDecimal.valueOf(4), order.getFilledQuantity());
    }
    @Test
    void shouldCompletePartiallyFilledOrder(){
        Order order = OrderFixture.buyOrder(BigDecimal.valueOf(10));
        order.applyFill(BigDecimal.valueOf(4));
        order.applyFill(BigDecimal.valueOf(6));
        assertEquals(OrderStatus.FILLED,order.getStatus());
        assertEquals(BigDecimal.valueOf(10),order.getFilledQuantity());
        assertEquals(BigDecimal.ZERO,order.getRemainingQuantity());
    }
    @Test
    void shouldRejectCancellationOfAlreadyCancelledOrder(){
        Order order = OrderFixture.buyOrder(BigDecimal.valueOf(10));
        order.cancel();
        assertThrows(IllegalStateException.class, ()-> order.cancel());
    }
}